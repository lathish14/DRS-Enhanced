package drs.server.dao;

import drs.common.ResponseLog;
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
 * Data Access Object for the {@code response_logs} table (the audit trail).
 * Provides insert and list operations for the accountability log.
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ResponseLogDAO {

    /**
     * Inserts an audit log entry and returns it with the generated ID set.
     *
     * @param log the log entry to insert
     * @return the same log with its generated ID populated
     * @throws SQLException if the insert fails
     */
    public ResponseLog insert(ResponseLog log) throws SQLException {
        String sql = "INSERT INTO response_logs "
                + "(disaster_id, action_by, action_description, "
                + "action_type, log_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getDisasterId());
            ps.setString(2, log.getActionBy());
            ps.setString(3, log.getActionDescription());
            ps.setString(4, log.getActionType());
            ps.setTimestamp(5, Timestamp.valueOf(log.getLogTime()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setLogId(keys.getInt(1));
                }
            }
        }
        return log;
    } // end method insert

    /**
     * Returns all audit log entries ordered oldest first (so the client can
     * render newest-first by iterating in reverse, matching the original UI).
     *
     * @return list of all log entries
     * @throws SQLException if the query fails
     */
    public List<ResponseLog> findAll() throws SQLException {
        List<ResponseLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM response_logs ORDER BY log_id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(this.mapRow(rs));
            }
        }
        return logs;
    } // end method findAll

    /**
     * Returns all audit log entries for a specific disaster, oldest first.
     *
     * @param disasterId the disaster whose logs to retrieve
     * @return list of log entries for that disaster
     * @throws SQLException if the query fails
     */
    public List<ResponseLog> findByDisaster(int disasterId)
            throws SQLException {
        List<ResponseLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM response_logs "
                + "WHERE disaster_id = ? ORDER BY log_id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disasterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(this.mapRow(rs));
                }
            }
        }
        return logs;
    } // end method findByDisaster

    /**
     * Maps the current row of a result set to a ResponseLog object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped ResponseLog
     * @throws SQLException if a column read fails
     */
    private ResponseLog mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("log_time");
        return new ResponseLog(
                rs.getInt("log_id"),
                rs.getInt("disaster_id"),
                rs.getString("action_by"),
                rs.getString("action_description"),
                rs.getString("action_type"),
                ts.toLocalDateTime());
    } // end method mapRow

} // end class ResponseLogDAO
