package drs.server.dao;

import drs.common.User;
import drs.server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code users} table. Encapsulates all SQL needed
 * to authenticate users, list accounts, add accounts, and toggle the active
 * flag, keeping JDBC concerns out of the service layer.
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class UserDAO {

    /**
     * Finds an active user matching the supplied credentials.
     *
     * @param username the login username
     * @param password the login password
     * @return the matching User, or null if no active account matches
     * @throws SQLException if the query fails
     */
    public User authenticate(String username, String password)
            throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? "
                + "AND password = ? AND active = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return this.mapRow(rs);
                }
            }
        }
        return null;
    } // end method authenticate

    /**
     * Returns all user accounts ordered by user ID.
     *
     * @return list of all users
     * @throws SQLException if the query fails
     */
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(this.mapRow(rs));
            }
        }
        return users;
    } // end method findAll

    /**
     * Returns true if a username already exists (case-sensitive match).
     *
     * @param username the username to check
     * @return true if taken, false if available
     * @throws SQLException if the query fails
     */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    } // end method usernameExists

    /**
     * Inserts a new user and returns it with the database-assigned ID set.
     *
     * @param user the user to insert
     * @return the same user with its generated ID populated
     * @throws SQLException if the insert fails
     */
    public User insert(User user) throws SQLException {
        String sql = "INSERT INTO users "
                + "(full_name, username, password, role, department, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getDepartment());
            ps.setBoolean(6, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
        }
        return user;
    } // end method insert

    /**
     * Updates the active flag of a user account.
     *
     * @param userId the ID of the user to update
     * @param active the new active value
     * @return true if a row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean setActive(int userId, boolean active)
            throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    } // end method setActive

    /**
     * Maps the current row of a result set to a User object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped User
     * @throws SQLException if a column read fails
     */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("department"),
                rs.getBoolean("active"));
    } // end method mapRow

} // end class UserDAO
