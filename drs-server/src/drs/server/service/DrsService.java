package drs.server.service;

import drs.common.Department;
import drs.common.Disaster;
import drs.common.Dispatch;
import drs.common.Resource;
import drs.common.ResponseLog;
import drs.common.Stats;
import drs.common.User;
import drs.server.dao.DepartmentDAO;
import drs.server.dao.DisasterDAO;
import drs.server.dao.DispatchDAO;
import drs.server.dao.ResourceDAO;
import drs.server.dao.ResponseLogDAO;
import drs.server.dao.UserDAO;
import drs.server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central business logic service for the DRS server tier. This class is the
 * server-side equivalent of the Assessment Two {@code DisasterController}: it
 * holds all domain rules (validation, priority derivation, department
 * notification logic, resource dispatch transactions, statistics) but now
 * persists everything to MySQL through the DAO layer rather than to in-memory
 * lists.
 *
 * <p>A single shared instance is created by the server and used by all client
 * handler threads, so the methods are written to be safe for concurrent use:
 * each call uses its own short-lived JDBC connections, and the multi-step
 * dispatch/recall operations run inside explicit database transactions guarded
 * by a lock so deployed-unit counts never race.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DrsService {

    // --- DAOs ---

    /** DAO for user accounts. */
    private final UserDAO userDao = new UserDAO();

    /** DAO for departments. */
    private final DepartmentDAO departmentDao = new DepartmentDAO();

    /** DAO for disasters. */
    private final DisasterDAO disasterDao = new DisasterDAO();

    /** DAO for audit log entries. */
    private final ResponseLogDAO logDao = new ResponseLogDAO();

    /** DAO for resources (Feature 1). */
    private final ResourceDAO resourceDao = new ResourceDAO();

    /** DAO for dispatches (Feature 1). */
    private final DispatchDAO dispatchDao = new DispatchDAO();

    /**
     * Lock guarding the multi-statement dispatch and recall transactions so
     * that concurrent allocations cannot oversubscribe a resource.
     */
    private final Object dispatchLock = new Object();

    // --- Authentication ---

    /**
     * Authenticates a user. On success the returned User has its password
     * field cleared before being handed back to the network layer.
     *
     * @param username the login username
     * @param password the login password
     * @return the authenticated User (password cleared), or null on failure
     * @throws SQLException if the lookup fails
     */
    public User login(String username, String password) throws SQLException {
        User user = this.userDao.authenticate(username, password);
        if (user != null) {
            user.setPassword("");
        }
        return user;
    } // end method login

    // --- Disaster reporting ---

    /**
     * Validates and stores a new disaster report, then writes an audit log
     * entry for the action.
     *
     * @param disasterType the disaster category
     * @param location     the disaster location
     * @param description  the disaster description
     * @param reportedBy   the reporter's full name
     * @param actor        the username performing the action (for the log)
     * @return the stored Disaster with its database ID
     * @throws IllegalArgumentException if any required field is blank
     * @throws SQLException             if persistence fails
     */
    public Disaster reportDisaster(String disasterType, String location,
                                   String description, String reportedBy,
                                   String actor)
            throws SQLException {
        if (isBlank(disasterType)) {
            throw new IllegalArgumentException("Disaster type is required.");
        }
        if (isBlank(location)) {
            throw new IllegalArgumentException("Location is required.");
        }
        if (isBlank(description)) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (isBlank(reportedBy)) {
            throw new IllegalArgumentException("Reporter name is required.");
        }

        Disaster disaster =
                new Disaster(disasterType, location, description, reportedBy);
        this.disasterDao.insert(disaster);

        String who = isBlank(actor) ? reportedBy : actor;
        this.recordLog(disaster.getDisasterId(), who,
                "Disaster reported: " + disasterType + " at " + location,
                ResponseLog.ACTION_REPORTED);
        return disaster;
    } // end method reportDisaster

    /**
     * Returns all disasters.
     *
     * @return list of all disasters
     * @throws SQLException if the query fails
     */
    public List<Disaster> getAllDisasters() throws SQLException {
        return this.disasterDao.findAll();
    } // end method getAllDisasters

    // --- Assessment ---

    /**
     * Assesses a disaster: stores a severity score (1-10), derives priority,
     * sets status to UNDER ASSESSMENT, and logs the action.
     *
     * @param disasterId the disaster to assess
     * @param severity   the severity score (must be 1-10)
     * @param actor      the acting username (for the log)
     * @return true if the disaster existed and was updated
     * @throws IllegalArgumentException if severity is out of range
     * @throws SQLException             if persistence fails
     */
    public boolean assessDisaster(int disasterId, int severity, String actor)
            throws SQLException {
        if (severity < 1 || severity > 10) {
            throw new IllegalArgumentException(
                    "Severity score must be between 1 and 10.");
        }
        Disaster disaster = this.disasterDao.findById(disasterId);
        if (disaster == null) {
            return false;
        }
        disaster.setSeverityScore(severity);
        boolean ok = this.disasterDao.updateAssessment(
                disasterId, severity, disaster.getPriorityLevel(),
                Disaster.STATUS_UNDER_ASSESSMENT);
        if (ok) {
            this.recordLog(disasterId, safeActor(actor),
                    "Assessment completed. Severity: " + severity
                    + ", Priority set to: " + disaster.getPriorityLevel(),
                    ResponseLog.ACTION_ASSESSED);
        }
        return ok;
    } // end method assessDisaster

    // --- Department notification ---

    /**
     * Notifies the relevant departments for a disaster based on its type and
     * severity, updates the disaster's notified-departments list and status to
     * RESPONDING, marks each notified department NOTIFIED, and logs the action.
     *
     * <p>Notification rules (unchanged from Assessment Two):</p>
     * <ul>
     *   <li>Fire &amp; Emergency for Fire or Hurricane.</li>
     *   <li>Hospital for any disaster with severity 5 or above.</li>
     *   <li>Law Enforcement for any HIGH priority disaster.</li>
     *   <li>Any department whose specialization matches the disaster type.</li>
     * </ul>
     *
     * @param disasterId the disaster to coordinate
     * @param actor      the acting username (for the log)
     * @return the list of notified department names (empty if not found)
     * @throws SQLException if persistence fails
     */
    public List<String> notifyDepartments(int disasterId, String actor)
            throws SQLException {
        List<String> notified = new java.util.ArrayList<>();
        Disaster disaster = this.disasterDao.findById(disasterId);
        if (disaster == null) {
            return notified;
        }
        List<Department> departments = this.departmentDao.findAll();
        for (Department dept : departments) {
            boolean shouldNotify = false;
            String type = disaster.getDisasterType();

            if ("Fire & Emergency".equals(dept.getDepartmentName())
                    && ("Fire".equalsIgnoreCase(type)
                        || "Hurricane".equalsIgnoreCase(type))) {
                shouldNotify = true;
            }
            if ("Hospital".equals(dept.getDepartmentName())
                    && disaster.getSeverityScore() >= 5) {
                shouldNotify = true;
            }
            if ("Law Enforcement".equals(dept.getDepartmentName())
                    && Disaster.PRIORITY_HIGH.equals(
                            disaster.getPriorityLevel())) {
                shouldNotify = true;
            }
            if (dept.getSpecialization() != null
                    && dept.getSpecialization().equalsIgnoreCase(type)) {
                shouldNotify = true;
            }
            if (shouldNotify) {
                this.departmentDao.updateStatusByName(
                        dept.getDepartmentName(), Department.STATUS_NOTIFIED);
                notified.add(dept.getDepartmentName());
            }
        }
        String joined = String.join(", ", notified);
        this.disasterDao.updateNotification(
                disasterId, joined, Disaster.STATUS_RESPONDING);
        this.recordLog(disasterId, safeActor(actor),
                "Departments notified: " + joined,
                ResponseLog.ACTION_DEPT_NOTIFIED);
        return notified;
    } // end method notifyDepartments

    /**
     * Updates a disaster's lifecycle status and logs the change.
     *
     * @param disasterId the disaster to update
     * @param newStatus  the new status
     * @param actor      the acting username (for the log)
     * @return true if the disaster existed and was updated
     * @throws SQLException if persistence fails
     */
    public boolean updateStatus(int disasterId, String newStatus, String actor)
            throws SQLException {
        Disaster disaster = this.disasterDao.findById(disasterId);
        if (disaster == null) {
            return false;
        }
        String oldStatus = disaster.getStatus();
        boolean ok = this.disasterDao.updateStatus(disasterId, newStatus);
        if (ok) {
            this.recordLog(disasterId, safeActor(actor),
                    "Status updated from " + oldStatus + " to " + newStatus,
                    ResponseLog.ACTION_STATUS_UPDATE);
        }
        return ok;
    } // end method updateStatus

    // --- Departments / logs / users ---

    /**
     * Returns all departments.
     *
     * @return list of departments
     * @throws SQLException if the query fails
     */
    public List<Department> getAllDepartments() throws SQLException {
        return this.departmentDao.findAll();
    } // end method getAllDepartments

    /**
     * Returns all audit log entries.
     *
     * @return list of logs
     * @throws SQLException if the query fails
     */
    public List<ResponseLog> getAllLogs() throws SQLException {
        return this.logDao.findAll();
    } // end method getAllLogs

    /**
     * Returns all users (passwords cleared for transport).
     *
     * @return list of users
     * @throws SQLException if the query fails
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = this.userDao.findAll();
        for (User u : users) {
            u.setPassword("");
        }
        return users;
    } // end method getAllUsers

    /**
     * Adds a new user account after checking the username is free.
     *
     * @param user the user to add
     * @return the stored user (with ID and cleared password)
     * @throws IllegalArgumentException if the username is already taken
     * @throws SQLException             if persistence fails
     */
    public User addUser(User user) throws SQLException {
        if (this.userDao.usernameExists(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists: " + user.getUsername());
        }
        User saved = this.userDao.insert(user);
        saved.setPassword("");
        return saved;
    } // end method addUser

    /**
     * Self-registers a new user account. Identical to {@link #addUser} but
     * callable without an existing session, so the register screen can submit
     * without being logged in first. Validates that the username is free and
     * that all required fields are supplied.
     *
     * @param user the new user to register
     * @return the stored user (with ID, password cleared)
     * @throws IllegalArgumentException if any field is blank or username taken
     * @throws SQLException             if persistence fails
     */
    public User register(User user) throws SQLException {
        if (isBlank(user.getFullName())) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (isBlank(user.getUsername())) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (isBlank(user.getPassword())) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (isBlank(user.getRole())) {
            throw new IllegalArgumentException("Role is required.");
        }
        return this.addUser(user);
    } // end method register

    /**
     * Sets a user's active flag.
     *
     * @param userId the user to update
     * @param active the new active value
     * @return true if a row was updated
     * @throws SQLException if persistence fails
     */
    public boolean setUserActive(int userId, boolean active)
            throws SQLException {
        return this.userDao.setActive(userId, active);
    } // end method setUserActive

    // --- Feature 1: resources & dispatch ---

    /**
     * Returns all resources with availability figures.
     *
     * @return list of resources
     * @throws SQLException if the query fails
     */
    public List<Resource> getAllResources() throws SQLException {
        return this.resourceDao.findAll();
    } // end method getAllResources

    /**
     * Returns the dispatch history for a disaster.
     *
     * @param disasterId the disaster to query
     * @return list of dispatches
     * @throws SQLException if the query fails
     */
    public List<Dispatch> getDispatchesForDisaster(int disasterId)
            throws SQLException {
        return this.dispatchDao.findByDisaster(disasterId);
    } // end method getDispatchesForDisaster

    /**
     * Dispatches a quantity of a resource to a disaster inside a single
     * database transaction: it re-reads the resource under a lock, checks that
     * enough units are available, inserts the dispatch row, and increments the
     * resource's deployed-unit count. Either both writes succeed or both roll
     * back.
     *
     * @param disasterId the target disaster
     * @param resourceId the resource to dispatch
     * @param units      the number of units to allocate (must be positive)
     * @param actor      the authorising coordinator's username
     * @return the created Dispatch
     * @throws IllegalArgumentException if units invalid or insufficient stock
     * @throws SQLException             if the transaction fails
     */
    public Dispatch dispatchResource(int disasterId, int resourceId,
                                     int units, String actor)
            throws SQLException {
        if (units <= 0) {
            throw new IllegalArgumentException(
                    "Units to dispatch must be greater than zero.");
        }
        synchronized (this.dispatchLock) {
            Connection conn = null;
            try {
                conn = DatabaseConfig.getConnection();
                conn.setAutoCommit(false);

                Disaster disaster = this.disasterDao.findById(disasterId);
                if (disaster == null) {
                    throw new IllegalArgumentException(
                            "Disaster #" + disasterId + " not found.");
                }
                Resource resource =
                        this.resourceDao.findById(conn, resourceId);
                if (resource == null) {
                    throw new IllegalArgumentException(
                            "Resource #" + resourceId + " not found.");
                }
                if (units > resource.getAvailableUnits()) {
                    throw new IllegalArgumentException(
                            "Only " + resource.getAvailableUnits()
                            + " unit(s) of " + resource.getResourceName()
                            + " available.");
                }

                Dispatch dispatch = new Dispatch(disasterId, resourceId,
                        resource.getResourceName(),
                        resource.getDepartmentName(),
                        units, safeActor(actor));
                this.dispatchDao.insert(conn, dispatch);
                this.resourceDao.adjustDeployedUnits(conn, resourceId, units);

                conn.commit();

                this.recordLog(disasterId, safeActor(actor),
                        "Dispatched " + units + "x "
                        + resource.getResourceName()
                        + " from " + resource.getDepartmentName(),
                        ResponseLog.ACTION_RESOURCE_DISPATCH);
                return dispatch;
            } catch (SQLException | RuntimeException ex) {
                rollbackQuietly(conn);
                throw ex;
            } finally {
                closeQuietly(conn);
            }
        }
    } // end method dispatchResource

    /**
     * Recalls a deployed dispatch inside a transaction: marks it RECALLED and
     * returns its units to the owning resource.
     *
     * @param dispatchId the dispatch to recall
     * @param actor      the acting username (for the log)
     * @return true if the dispatch existed and was recalled
     * @throws SQLException if the transaction fails
     */
    public boolean recallDispatch(int dispatchId, String actor)
            throws SQLException {
        synchronized (this.dispatchLock) {
            Connection conn = null;
            try {
                conn = DatabaseConfig.getConnection();
                conn.setAutoCommit(false);

                Dispatch dispatch =
                        this.dispatchDao.findById(conn, dispatchId);
                if (dispatch == null
                        || Dispatch.STATUS_RECALLED.equals(
                                dispatch.getStatus())) {
                    rollbackQuietly(conn);
                    return false;
                }
                this.dispatchDao.updateStatus(
                        conn, dispatchId, Dispatch.STATUS_RECALLED);
                this.resourceDao.adjustDeployedUnits(
                        conn, dispatch.getResourceId(),
                        -dispatch.getUnitsDispatched());
                conn.commit();

                this.recordLog(dispatch.getDisasterId(), safeActor(actor),
                        "Recalled " + dispatch.getUnitsDispatched() + "x "
                        + dispatch.getResourceName(),
                        ResponseLog.ACTION_RESOURCE_DISPATCH);
                return true;
            } catch (SQLException | RuntimeException ex) {
                rollbackQuietly(conn);
                throw ex;
            } finally {
                closeQuietly(conn);
            }
        }
    } // end method recallDispatch

    // --- Statistics ---

    /**
     * Computes a fresh statistics snapshot from the current dataset.
     *
     * @return a populated Stats object
     * @throws SQLException if a query fails
     */
    public Stats getStatistics() throws SQLException {
        List<Disaster> all = this.disasterDao.findAll();
        Stats stats = new Stats();
        stats.setTotalCount(all.size());

        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> byPriority = new HashMap<>();
        int high = 0;
        int medium = 0;
        int resolved = 0;
        long severitySum = 0;

        for (Disaster d : all) {
            byType.merge(d.getDisasterType(), 1, Integer::sum);
            byPriority.merge(d.getPriorityLevel(), 1, Integer::sum);
            if (Disaster.PRIORITY_HIGH.equals(d.getPriorityLevel())) {
                high++;
            } else if (Disaster.PRIORITY_MEDIUM.equals(
                    d.getPriorityLevel())) {
                medium++;
            }
            if (Disaster.STATUS_RESOLVED.equals(d.getStatus())) {
                resolved++;
            }
            severitySum += d.getSeverityScore();
        }

        stats.setHighPriorityCount(high);
        stats.setMediumPriorityCount(medium);
        stats.setResolvedCount(resolved);
        stats.setActiveCount(all.size() - resolved);
        stats.setAverageSeverity(
                all.isEmpty() ? 0.0 : (double) severitySum / all.size());
        stats.setCountByType(byType);
        stats.setCountByPriority(byPriority);
        stats.setTotalUnitsDeployed(this.resourceDao.totalDeployedUnits());
        return stats;
    } // end method getStatistics

    // --- Private helpers ---

    /**
     * Writes an audit log entry. Failures here are logged to stderr but never
     * abort the primary operation that triggered the log.
     *
     * @param disasterId  the related disaster ID
     * @param actor       the acting username
     * @param description the action description
     * @param type        the action type constant
     */
    private void recordLog(int disasterId, String actor,
                           String description, String type) {
        try {
            this.logDao.insert(
                    new ResponseLog(disasterId, actor, description, type));
        } catch (SQLException ex) {
            System.err.println("[Service] Failed to write audit log: "
                    + ex.getMessage());
        }
    } // end method recordLog

    /**
     * Returns true if a string is null or blank after trimming.
     *
     * @param value the string to test
     * @return true if null/blank
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    } // end method isBlank

    /**
     * Returns the actor name, or "SYSTEM" if it is blank.
     *
     * @param actor the candidate actor
     * @return a non-blank actor string
     */
    private static String safeActor(String actor) {
        return isBlank(actor) ? "SYSTEM" : actor;
    } // end method safeActor

    /**
     * Rolls back a connection, swallowing any error.
     *
     * @param conn the connection to roll back (may be null)
     */
    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // best-effort rollback
            }
        }
    } // end method rollbackQuietly

    /**
     * Restores auto-commit and closes a connection, swallowing any error.
     *
     * @param conn the connection to close (may be null)
     */
    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
                // best-effort close
            }
        }
    } // end method closeQuietly

} // end class DrsService
