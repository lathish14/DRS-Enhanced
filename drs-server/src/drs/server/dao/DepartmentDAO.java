package drs.server.dao;

import drs.common.Department;
import drs.server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code departments} table. Handles listing
 * departments and updating their operational response status.
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DepartmentDAO {

    /**
     * Returns all departments ordered by department ID.
     *
     * @return list of all departments
     * @throws SQLException if the query fails
     */
    public List<Department> findAll() throws SQLException {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT * FROM departments ORDER BY department_id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                departments.add(this.mapRow(rs));
            }
        }
        return departments;
    } // end method findAll

    /**
     * Updates the response status of a department by name.
     *
     * @param departmentName the department to update
     * @param status         the new response status
     * @return true if a row was updated, false otherwise
     * @throws SQLException if the update fails
     */
    public boolean updateStatusByName(String departmentName, String status)
            throws SQLException {
        String sql = "UPDATE departments SET response_status = ? "
                + "WHERE department_name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, departmentName);
            return ps.executeUpdate() > 0;
        }
    } // end method updateStatusByName

    /**
     * Maps the current row of a result set to a Department object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped Department
     * @throws SQLException if a column read fails
     */
    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
                rs.getInt("department_id"),
                rs.getString("department_name"),
                rs.getString("contact_number"),
                rs.getString("contact_email"),
                rs.getString("specialization"),
                rs.getString("response_status"));
    } // end method mapRow

} // end class DepartmentDAO
