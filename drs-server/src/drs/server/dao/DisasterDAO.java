package drs.server.dao;

import drs.common.Disaster;
import drs.server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code disasters} table. Provides insert, list,
 * find-by-id, and targeted update operations used by the business service.
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DisasterDAO {

    /**
     * Inserts a new disaster report and returns it with the database-assigned
     * ID populated.
     *
     * @param disaster the disaster to insert
     * @return the same disaster with its generated ID set
     * @throws SQLException if the insert fails
     */
    public Disaster insert(Disaster disaster) throws SQLException {
        String sql = "INSERT INTO disasters "
                + "(disaster_type, location, description, severity_score, "
                + "priority_level, status, reported_at, reported_by, "
                + "notified_departments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, disaster.getDisasterType());
            ps.setString(2, disaster.getLocation());
            ps.setString(3, disaster.getDescription());
            ps.setInt(4, disaster.getSeverityScore());
            ps.setString(5, disaster.getPriorityLevel());
            ps.setString(6, disaster.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(disaster.getReportedAt()));
            ps.setString(8, disaster.getReportedBy());
            ps.setString(9, disaster.getNotifiedDepartments());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    disaster.setDisasterId(keys.getInt(1));
                }
            }
        }
        return disaster;
    } // end method insert

    /**
     * Returns all disasters ordered by most recent first.
     *
     * @return list of all disasters
     * @throws SQLException if the query fails
     */
    public List<Disaster> findAll() throws SQLException {
        List<Disaster> disasters = new ArrayList<>();
        String sql = "SELECT * FROM disasters ORDER BY disaster_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                disasters.add(this.mapRow(rs));
            }
        }
        return disasters;
    } // end method findAll

    /**
     * Finds a single disaster by its ID.
     *
     * @param disasterId the ID to look up
     * @return the matching Disaster, or null if not found
     * @throws SQLException if the query fails
     */
    public Disaster findById(int disasterId) throws SQLException {
        String sql = "SELECT * FROM disasters WHERE disaster_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disasterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return this.mapRow(rs);
                }
            }
        }
        return null;
    } // end method findById

    /**
     * Applies an assessment: stores the severity score, derived priority, and
     * the UNDER ASSESSMENT status for a disaster.
     *
     * @param disasterId the disaster to update
     * @param severity   the severity score
     * @param priority   the derived priority level
     * @param status     the new status
     * @return true if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateAssessment(int disasterId, int severity,
                                    String priority, String status)
            throws SQLException {
        String sql = "UPDATE disasters SET severity_score = ?, "
                + "priority_level = ?, status = ? WHERE disaster_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, severity);
            ps.setString(2, priority);
            ps.setString(3, status);
            ps.setInt(4, disasterId);
            return ps.executeUpdate() > 0;
        }
    } // end method updateAssessment

    /**
     * Updates only the lifecycle status of a disaster.
     *
     * @param disasterId the disaster to update
     * @param status     the new status
     * @return true if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateStatus(int disasterId, String status)
            throws SQLException {
        String sql = "UPDATE disasters SET status = ? WHERE disaster_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, disasterId);
            return ps.executeUpdate() > 0;
        }
    } // end method updateStatus

    /**
     * Updates the notified-departments list and status after notification.
     *
     * @param disasterId          the disaster to update
     * @param notifiedDepartments the comma-separated notified names
     * @param status              the new status (typically RESPONDING)
     * @return true if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateNotification(int disasterId,
                                      String notifiedDepartments,
                                      String status) throws SQLException {
        String sql = "UPDATE disasters SET notified_departments = ?, "
                + "status = ? WHERE disaster_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notifiedDepartments);
            ps.setString(2, status);
            ps.setInt(3, disasterId);
            return ps.executeUpdate() > 0;
        }
    } // end method updateNotification

    /**
     * Maps the current row of a result set to a Disaster object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped Disaster
     * @throws SQLException if a column read fails
     */
    private Disaster mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("reported_at");
        return new Disaster(
                rs.getInt("disaster_id"),
                rs.getString("disaster_type"),
                rs.getString("location"),
                rs.getString("description"),
                rs.getInt("severity_score"),
                rs.getString("status"),
                ts.toLocalDateTime(),
                rs.getString("reported_by"),
                rs.getString("notified_departments"));
    } // end method mapRow

} // end class DisasterDAO
