package drs.server.dao;

import drs.common.Resource;
import drs.server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code resources} table, supporting Enhanced
 * Feature 1 (Resource &amp; Dispatch Management). Joins to {@code departments}
 * so each resource carries its owning department's display name.
 *
 * <p>The {@link #adjustDeployedUnits(Connection, int, int)} method takes an
 * existing connection so that dispatch creation and the matching deployed-unit
 * increment can occur inside a single database transaction, preserving
 * consistency under the concurrent access the multi-threaded server allows.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ResourceDAO {

    /**
     * Returns all resources with their owning department name, ordered by
     * department then resource name.
     *
     * @return list of all resources
     * @throws SQLException if the query fails
     */
    public List<Resource> findAll() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        String sql = "SELECT r.resource_id, r.department_id, "
                + "d.department_name, r.resource_name, r.resource_type, "
                + "r.total_units, r.deployed_units "
                + "FROM resources r "
                + "JOIN departments d ON r.department_id = d.department_id "
                + "ORDER BY d.department_name, r.resource_name";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resources.add(this.mapRow(rs));
            }
        }
        return resources;
    } // end method findAll

    /**
     * Finds a single resource by ID using the supplied connection (so it can
     * participate in a dispatch transaction). Returns the full row including
     * the owning department name.
     *
     * @param conn       an open connection (transaction-scoped)
     * @param resourceId the resource ID to look up
     * @return the matching Resource, or null if not found
     * @throws SQLException if the query fails
     */
    public Resource findById(Connection conn, int resourceId)
            throws SQLException {
        String sql = "SELECT r.resource_id, r.department_id, "
                + "d.department_name, r.resource_name, r.resource_type, "
                + "r.total_units, r.deployed_units "
                + "FROM resources r "
                + "JOIN departments d ON r.department_id = d.department_id "
                + "WHERE r.resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return this.mapRow(rs);
                }
            }
        }
        return null;
    } // end method findById

    /**
     * Adjusts the deployed-unit count of a resource by a (signed) delta using
     * the supplied transaction connection. A positive delta deploys units; a
     * negative delta recalls them.
     *
     * @param conn       the transaction-scoped connection
     * @param resourceId the resource to adjust
     * @param delta      the signed change to deployed_units
     * @return true if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean adjustDeployedUnits(Connection conn, int resourceId,
                                       int delta) throws SQLException {
        String sql = "UPDATE resources "
                + "SET deployed_units = deployed_units + ? "
                + "WHERE resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, resourceId);
            return ps.executeUpdate() > 0;
        }
    } // end method adjustDeployedUnits

    /**
     * Returns the total number of resource units currently deployed across all
     * resources. Used by the statistics snapshot.
     *
     * @return total deployed units
     * @throws SQLException if the query fails
     */
    public int totalDeployedUnits() throws SQLException {
        String sql = "SELECT COALESCE(SUM(deployed_units), 0) FROM resources";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    } // end method totalDeployedUnits

    /**
     * Maps the current row of a result set to a Resource object.
     *
     * @param rs the result set positioned on a valid row
     * @return the mapped Resource
     * @throws SQLException if a column read fails
     */
    private Resource mapRow(ResultSet rs) throws SQLException {
        return new Resource(
                rs.getInt("resource_id"),
                rs.getInt("department_id"),
                rs.getString("department_name"),
                rs.getString("resource_name"),
                rs.getString("resource_type"),
                rs.getInt("total_units"),
                rs.getInt("deployed_units"));
    } // end method mapRow

} // end class ResourceDAO
