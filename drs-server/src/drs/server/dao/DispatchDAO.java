package drs.server.dao;

import drs.common.Dispatch;
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
 * Data Access Object for the {@code dispatches} table, supporting Enhanced
 * Feature 1 (Resource &amp; Dispatch Management).
 *
 * <p>The {@link #insert(Connection, Dispatch)} and
 * {@link #updateStatus(Connection, int, String)} methods accept an existing
 * connection so dispatch writes can run inside the same transaction that
 * adjusts a resource's deployed-unit count, ensuring the two stay consistent
 * even under concurrent client requests handled on separate server threads.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DispatchDAO {

    /**
     * Inserts a new dispatch row using the supplied transaction connection and
     * returns it with the generated ID populated.
     *
     * @param conn     the transaction-scoped connection
     * @param dispatch the dispatch to insert
     * @return the same dispatch with its generated ID set
     * @throws SQLException if the insert fails
     */
    public Dispatch insert(Connection conn, Dispatch dispatch)
            throws SQLException {
        String sql = "INSERT INTO dispatches "
                + "(disaster_id, resource_id, units_dispatched, "
                + "dispatched_by, status, dispatched_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, dispatch.getDisasterId());
            ps.setInt(2, dispatch.getResourceId());
            ps.setInt(3, dispatch.getUnitsDispatched());
            ps.setString(4, dispatch.getDispatchedBy());
            ps.setString(5, dispatch.getStatus());
            ps.setTimestamp(6,
                    Timestamp.valueOf(dispatch.getDispatchedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    dispatch.setDispatchId(keys.getInt(1));
                }
            }
        }
        return dispatch;
    } // end method insert

    /**
     * Returns all dispatches for a given disaster, newest first, with resource
     * and department display names joined in.
     *
     * @param disasterId the disaster whose dispatches to list
     * @return list of dispatches for the disaster
     * @throws SQLException if the query fails
     */
    public List<Dispatch> findByDisaster(int disasterId)
            throws SQLException {
        List<Dispatch> dispatches = new ArrayList<>();
        String sql = "SELECT di.dispatch_id, di.disaster_id, di.resource_id, "
                + "r.resource_name, d.department_name, di.units_dispatched, "
                + "di.dispatched_by, di.status, di.dispatched_at "
                + "FROM dispatches di "
                + "JOIN resources r ON di.resource_id = r.resource_id "
                + "JOIN departments d ON r.department_id = d.department_id "
                + "WHERE di.disaster_id = ? "
                + "ORDER BY di.dispatch_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disasterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dispatches.add(this.mapRow(rs));
                }
            }
        }
        return dispatches;
    } // end method findByDisaster

    /**
     * Finds a dispatch by ID using the supplied transaction connection.
     *
     * @param conn       the transaction-scoped connection
     * @param dispatchId the dispatch ID to look up
     * @return the matching Dispatch, or null if not found
     * @throws SQLException if the query fails
     */
    public Dispatch findById(Connection conn, int dispatchId)
            throws SQLException {
        String sql = "SELECT di.dispatch_id, di.disaster_id, di.resource_id, "
                + "r.resource_name, d.department_name, di.units_dispatched, "
                + "di.dispatched_by, di.status, di.dispatched_at "
                + "FROM dispatches di "
                + "JOIN resources r ON di.resource_id = r.resource_id "
                + "JOIN departments d ON r.department_id = d.department_id "
                + "WHERE di.dispatch_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dispatchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return this.mapRow(rs);
                }
            }
        }
        return null;
    } // end method findById

    /**
     * Updates the status of a dispatch using the supplied transaction
     * connection (used when recalling deployed units).
     *
     * @param conn       the transaction-scoped connection
     * @param dispatchId the dispatch to update
     * @param status     the new status
     * @return true if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateStatus(Connection conn, int dispatchId,
                                String status) throws SQLException {
        String sql = "UPDATE dispatches SET status = ? "
                + "WHERE dispatch_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, dispatchId);
            return ps.executeUpdate() > 0;
        }
    } // end method updateStatus

    /**
     * Maps the current row of a result set to a Dispatch object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped Dispatch
     * @throws SQLException if a column read fails
     */
    private Dispatch mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("dispatched_at");
        return new Dispatch(
                rs.getInt("dispatch_id"),
                rs.getInt("disaster_id"),
                rs.getInt("resource_id"),
                rs.getString("resource_name"),
                rs.getString("department_name"),
                rs.getInt("units_dispatched"),
                rs.getString("dispatched_by"),
                rs.getString("status"),
                ts.toLocalDateTime());
    } // end method mapRow

} // end class DispatchDAO
