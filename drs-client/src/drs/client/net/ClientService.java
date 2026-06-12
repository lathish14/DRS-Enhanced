package drs.client.net;

import drs.common.Department;
import drs.common.Disaster;
import drs.common.Dispatch;
import drs.common.Request;
import drs.common.Resource;
import drs.common.Response;
import drs.common.ResponseLog;
import drs.common.Stats;
import drs.common.User;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Typed client-side facade over {@link ServerConnection}. Each method builds a
 * {@link Request}, sends it, and unwraps the {@link Response}, so the JavaFX
 * controllers can call clean domain methods instead of dealing with the raw
 * request/response protocol.
 *
 * <p>This is the client analogue of the Assessment Two {@code
 * DisasterController}: it exposes the same operations, but every call is now
 * served by the remote multi-threaded server over a socket.</p>
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ClientService {

    /** The shared server connection used for all calls. */
    private final ServerConnection connection =
            ServerConnection.getInstance();

    /** The currently logged-in user (null until login succeeds). */
    private User currentUser;

    // --- Connection / session ---

    /**
     * Connects to the server if not already connected.
     *
     * @throws IOException if the connection fails
     */
    public void ensureConnected() throws IOException {
        if (!this.connection.isConnected()) {
            this.connection.connect();
        }
    } // end method ensureConnected

    /**
     * Registers a listener for live alerts pushed by the server.
     *
     * @param listener the alert consumer
     */
    public void setAlertListener(Consumer<drs.common.Alert> listener) {
        this.connection.setAlertListener(listener);
    } // end method setAlertListener

    /**
     * Subscribes this client connection to live alert broadcasts.
     *
     * @throws IOException if the request fails
     */
    public void subscribeAlerts() throws IOException {
        this.connection.send(new Request(Request.ACTION_SUBSCRIBE_ALERTS));
    } // end method subscribeAlerts

    /**
     * Returns the currently logged-in user.
     *
     * @return the current User, or null if not logged in
     */
    public User getCurrentUser() {
        return this.currentUser;
    } // end method getCurrentUser

    // --- Authentication ---

    /**
     * Attempts to log in with the given credentials. On success the current
     * user is stored and the session username is recorded on the connection.
     *
     * @param username the login username
     * @param password the login password
     * @return true if login succeeded
     * @throws IOException if the request fails
     */
    public boolean login(String username, String password)
            throws IOException {
        Response response = this.connection.send(
                new Request(Request.ACTION_LOGIN)
                        .put("username", username)
                        .put("password", password));
        if (response.isSuccess()) {
            this.currentUser = response.asUser();
            this.connection.setSessionUser(username);
            return true;
        }
        return false;
    } // end method login

    /**
     * Registers a new user account. No login session is required.
     *
     * @param fullName   the user's full name
     * @param username   the desired username
     * @param password   the chosen password
     * @param role       the chosen role
     * @param department the user's department or organisation
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response register(String fullName, String username,
                             String password, String role,
                             String department) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_REGISTER)
                        .put("fullName", fullName)
                        .put("username", username)
                        .put("password", password)
                        .put("role", role)
                        .put("department", department));
    } // end method register

    /**
     * Logs out the current user and clears the session.
     */
    public void logout() {
        try {
            this.connection.send(new Request(Request.ACTION_LOGOUT));
        } catch (IOException ignored) {
            // best effort
        }
        this.currentUser = null;
        this.connection.setSessionUser(null);
    } // end method logout

    // --- Disasters ---

    /**
     * Submits a new disaster report.
     *
     * @param type        the disaster type
     * @param location    the location
     * @param description the description
     * @param reportedBy  the reporter's full name
     * @return the server response (carries the stored Disaster on success)
     * @throws IOException if the request fails
     */
    public Response reportDisaster(String type, String location,
                                   String description, String reportedBy)
            throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_REPORT_DISASTER)
                        .put("type", type)
                        .put("location", location)
                        .put("description", description)
                        .put("reportedBy", reportedBy));
    } // end method reportDisaster

    /**
     * Retrieves all disasters from the server.
     *
     * @return list of disasters (empty on failure)
     * @throws IOException if the request fails
     */
    public List<Disaster> getDisasters() throws IOException {
        Response response = this.connection.send(
                new Request(Request.ACTION_GET_DISASTERS));
        return response.asList();
    } // end method getDisasters

    /**
     * Assesses a disaster with a severity score.
     *
     * @param disasterId the disaster to assess
     * @param severity   the severity score (1-10)
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response assess(int disasterId, int severity) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_ASSESS)
                        .put("disasterId", disasterId)
                        .put("severity", severity));
    } // end method assess

    /**
     * Notifies relevant departments for a disaster.
     *
     * @param disasterId the disaster to coordinate
     * @return the server response (carries notified names on success)
     * @throws IOException if the request fails
     */
    public Response notifyDepartments(int disasterId) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_NOTIFY)
                        .put("disasterId", disasterId));
    } // end method notifyDepartments

    /**
     * Updates a disaster's lifecycle status.
     *
     * @param disasterId the disaster to update
     * @param status     the new status
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response updateStatus(int disasterId, String status)
            throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_UPDATE_STATUS)
                        .put("disasterId", disasterId)
                        .put("status", status));
    } // end method updateStatus

    // --- Departments / logs / stats ---

    /**
     * Retrieves all departments.
     *
     * @return list of departments
     * @throws IOException if the request fails
     */
    public List<Department> getDepartments() throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_DEPARTMENTS)).asList();
    } // end method getDepartments

    /**
     * Retrieves the full audit log.
     *
     * @return list of log entries
     * @throws IOException if the request fails
     */
    public List<ResponseLog> getLogs() throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_LOGS)).asList();
    } // end method getLogs

    /**
     * Retrieves the statistics snapshot.
     *
     * @return the Stats object, or null on failure
     * @throws IOException if the request fails
     */
    public Stats getStats() throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_STATS)).asStats();
    } // end method getStats

    // --- Users (admin) ---

    /**
     * Retrieves all user accounts.
     *
     * @return list of users
     * @throws IOException if the request fails
     */
    public List<User> getUsers() throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_USERS)).asList();
    } // end method getUsers

    /**
     * Adds a new user account.
     *
     * @param fullName   the full name
     * @param username   the username
     * @param password   the password
     * @param role       the role
     * @param department the department
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response addUser(String fullName, String username,
                            String password, String role, String department)
            throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_ADD_USER)
                        .put("fullName", fullName)
                        .put("username", username)
                        .put("password", password)
                        .put("role", role)
                        .put("department", department));
    } // end method addUser

    /**
     * Activates or deactivates a user account.
     *
     * @param userId the user to update
     * @param active the new active flag
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response setUserActive(int userId, boolean active)
            throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_SET_USER_ACTIVE)
                        .put("userId", userId)
                        .put("active", String.valueOf(active)));
    } // end method setUserActive

    // --- Feature 1: resources & dispatch ---

    /**
     * Retrieves all resources with availability figures.
     *
     * @return list of resources
     * @throws IOException if the request fails
     */
    public List<Resource> getResources() throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_RESOURCES)).asList();
    } // end method getResources

    /**
     * Dispatches resource units to a disaster.
     *
     * @param disasterId the target disaster
     * @param resourceId the resource to dispatch
     * @param units      the number of units
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response dispatchResource(int disasterId, int resourceId,
                                     int units) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_DISPATCH)
                        .put("disasterId", disasterId)
                        .put("resourceId", resourceId)
                        .put("units", units));
    } // end method dispatchResource

    /**
     * Retrieves dispatches for a disaster.
     *
     * @param disasterId the disaster to query
     * @return list of dispatches
     * @throws IOException if the request fails
     */
    public List<Dispatch> getDispatches(int disasterId) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_GET_DISPATCHES)
                        .put("disasterId", disasterId)).asList();
    } // end method getDispatches

    /**
     * Recalls a deployed dispatch.
     *
     * @param dispatchId the dispatch to recall
     * @return the server response
     * @throws IOException if the request fails
     */
    public Response recallDispatch(int dispatchId) throws IOException {
        return this.connection.send(
                new Request(Request.ACTION_RECALL)
                        .put("dispatchId", dispatchId));
    } // end method recallDispatch

} // end class ClientService
