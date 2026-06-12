package drs.server.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the {@code drs_enhanced} schema and all tables programmatically, then
 * seeds the default departments, users, and a starter set of resources so the
 * DRS-Enhanced is ready to run immediately after launch.
 *
 * <p>The assignment requires that the database and tables be generated
 * programmatically and that every entity be assigned an ID on save. This class
 * issues {@code CREATE DATABASE IF NOT EXISTS} and {@code CREATE TABLE IF NOT
 * EXISTS} statements with {@code AUTO_INCREMENT} primary keys to satisfy both
 * requirements. Seeding is idempotent: it only inserts default rows when the
 * relevant table is empty, so restarting the server never duplicates data.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DatabaseInitialiser {

    /**
     * Runs the full initialisation sequence: create schema, create tables,
     * and seed default data. Safe to call on every server startup.
     *
     * @throws SQLException if any DDL or seed statement fails
     */
    public void initialise() throws SQLException {
        this.createSchema();
        this.createTables();
        this.seedDepartments();
        this.seedUsers();
        this.seedResources();
        System.out.println("[DB] Database initialised and ready: "
                + DatabaseConfig.DB_NAME);
    } // end method initialise

    /**
     * Creates the {@code drs_enhanced} database if it does not already exist.
     *
     * @throws SQLException if the CREATE DATABASE statement fails
     */
    private void createSchema() throws SQLException {
        try (Connection conn = DatabaseConfig.getServerConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS "
                    + DatabaseConfig.DB_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("[DB] Schema ensured: "
                    + DatabaseConfig.DB_NAME);
        }
    } // end method createSchema

    /**
     * Creates all tables used by the DRS-Enhanced if they do not exist.
     *
     * @throws SQLException if any CREATE TABLE statement fails
     */
    private void createTables() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // users — authentication and role-based access
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
                + "  user_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  full_name VARCHAR(100) NOT NULL,"
                + "  username VARCHAR(50) NOT NULL UNIQUE,"
                + "  password VARCHAR(100) NOT NULL,"
                + "  role VARCHAR(30) NOT NULL,"
                + "  department VARCHAR(80),"
                + "  active TINYINT(1) NOT NULL DEFAULT 1"
                + ") ENGINE=InnoDB");

            // departments — external responding organisations
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS departments ("
                + "  department_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  department_name VARCHAR(80) NOT NULL UNIQUE,"
                + "  contact_number VARCHAR(30),"
                + "  contact_email VARCHAR(120),"
                + "  specialization VARCHAR(60),"
                + "  response_status VARCHAR(30) NOT NULL DEFAULT 'STANDBY'"
                + ") ENGINE=InnoDB");

            // disasters — core reported events
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS disasters ("
                + "  disaster_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  disaster_type VARCHAR(50) NOT NULL,"
                + "  location VARCHAR(150) NOT NULL,"
                + "  description TEXT NOT NULL,"
                + "  severity_score INT NOT NULL DEFAULT 0,"
                + "  priority_level VARCHAR(10) NOT NULL DEFAULT 'LOW',"
                + "  status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',"
                + "  reported_at DATETIME NOT NULL,"
                + "  reported_by VARCHAR(100) NOT NULL,"
                + "  notified_departments VARCHAR(400) DEFAULT ''"
                + ") ENGINE=InnoDB");

            // response_logs — audit trail (Assessment 2 creative feature)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS response_logs ("
                + "  log_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  disaster_id INT NOT NULL,"
                + "  action_by VARCHAR(50) NOT NULL,"
                + "  action_description VARCHAR(400) NOT NULL,"
                + "  action_type VARCHAR(40) NOT NULL,"
                + "  log_time DATETIME NOT NULL,"
                + "  INDEX idx_logs_disaster (disaster_id)"
                + ") ENGINE=InnoDB");

            // resources — Enhanced Feature 1: deployable units per department
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS resources ("
                + "  resource_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  department_id INT NOT NULL,"
                + "  resource_name VARCHAR(80) NOT NULL,"
                + "  resource_type VARCHAR(40) NOT NULL,"
                + "  total_units INT NOT NULL DEFAULT 0,"
                + "  deployed_units INT NOT NULL DEFAULT 0,"
                + "  CONSTRAINT fk_resource_dept FOREIGN KEY (department_id)"
                + "    REFERENCES departments(department_id),"
                + "  INDEX idx_resource_dept (department_id)"
                + ") ENGINE=InnoDB");

            // dispatches — Enhanced Feature 1: allocations to disasters
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS dispatches ("
                + "  dispatch_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "  disaster_id INT NOT NULL,"
                + "  resource_id INT NOT NULL,"
                + "  units_dispatched INT NOT NULL,"
                + "  dispatched_by VARCHAR(50) NOT NULL,"
                + "  status VARCHAR(20) NOT NULL DEFAULT 'DEPLOYED',"
                + "  dispatched_at DATETIME NOT NULL,"
                + "  CONSTRAINT fk_dispatch_disaster FOREIGN KEY (disaster_id)"
                + "    REFERENCES disasters(disaster_id),"
                + "  CONSTRAINT fk_dispatch_resource FOREIGN KEY (resource_id)"
                + "    REFERENCES resources(resource_id),"
                + "  INDEX idx_dispatch_disaster (disaster_id)"
                + ") ENGINE=InnoDB");

            System.out.println("[DB] All tables ensured.");
        }
    } // end method createTables

    /**
     * Seeds the eight standard departments if the departments table is empty.
     *
     * @throws SQLException if a seed insert fails
     */
    private void seedDepartments() throws SQLException {
        if (!this.isTableEmpty("departments")) {
            return;
        }
        String sql = "INSERT INTO departments "
                + "(department_name, contact_number, contact_email, "
                + "specialization, response_status) VALUES "
                + "('Fire & Emergency','000','fire@drs.gov.au',"
                + "'Fire','STANDBY'),"
                + "('Hospital','131450','hospital@drs.gov.au',"
                + "'Medical','STANDBY'),"
                + "('Electricity','132051','electricity@drs.gov.au',"
                + "'Infrastructure','STANDBY'),"
                + "('Transportation','131500','transport@drs.gov.au',"
                + "'Transport','STANDBY'),"
                + "('Waste Management','1300123456','waste@drs.gov.au',"
                + "'Environmental','STANDBY'),"
                + "('Water Supply','132525','water@drs.gov.au',"
                + "'Infrastructure','STANDBY'),"
                + "('Law Enforcement','000','police@drs.gov.au',"
                + "'Security','STANDBY'),"
                + "('Schools','131872','schools@drs.gov.au',"
                + "'Community','STANDBY')";
        this.runUpdate(sql);
        System.out.println("[DB] Seeded 8 default departments.");
    } // end method seedDepartments

    /**
     * Seeds the four default user accounts if the users table is empty.
     *
     * @throws SQLException if a seed insert fails
     */
    private void seedUsers() throws SQLException {
        if (!this.isTableEmpty("users")) {
            return;
        }
        String sql = "INSERT INTO users "
                + "(full_name, username, password, role, department, active)"
                + " VALUES "
                + "('Admin User','admin','admin123','ADMIN',"
                + "'Administration',1),"
                + "('John Coordinator','coordinator','coord123',"
                + "'COORDINATOR','Emergency Management',1),"
                + "('Jane Public','public','pub123','PUBLIC',"
                + "'General Public',1),"
                + "('Sam Staff','staff','staff123','DEPARTMENT_STAFF',"
                + "'Fire & Emergency',1)";
        this.runUpdate(sql);
        System.out.println("[DB] Seeded 4 default users.");
    } // end method seedUsers

    /**
     * Seeds a starter set of resources for each department if the resources
     * table is empty. Department IDs are resolved by name to stay correct
     * regardless of AUTO_INCREMENT values.
     *
     * @throws SQLException if a seed insert fails
     */
    private void seedResources() throws SQLException {
        if (!this.isTableEmpty("resources")) {
            return;
        }
        // Resolve department IDs by name, then insert resources for each.
        this.insertResourceByDeptName(
                "Fire & Emergency", "Fire Truck", "Vehicle", 8);
        this.insertResourceByDeptName(
                "Fire & Emergency", "Rescue Team", "Personnel", 12);
        this.insertResourceByDeptName(
                "Hospital", "Ambulance", "Vehicle", 10);
        this.insertResourceByDeptName(
                "Hospital", "Medical Team", "Personnel", 15);
        this.insertResourceByDeptName(
                "Electricity", "Repair Crew", "Personnel", 6);
        this.insertResourceByDeptName(
                "Electricity", "Mobile Generator", "Equipment", 5);
        this.insertResourceByDeptName(
                "Transportation", "Evacuation Bus", "Vehicle", 7);
        this.insertResourceByDeptName(
                "Water Supply", "Water Tanker", "Vehicle", 4);
        this.insertResourceByDeptName(
                "Law Enforcement", "Patrol Unit", "Vehicle", 14);
        this.insertResourceByDeptName(
                "Waste Management", "Debris Truck", "Vehicle", 6);
        System.out.println("[DB] Seeded default resources.");
    } // end method seedResources

    /**
     * Inserts a single resource row, resolving the owning department's ID by
     * its name.
     *
     * @param deptName     the owning department's name
     * @param resourceName the resource name
     * @param resourceType the resource category
     * @param totalUnits   the total units available
     * @throws SQLException if the lookup or insert fails
     */
    private void insertResourceByDeptName(String deptName,
                                          String resourceName,
                                          String resourceType,
                                          int totalUnits)
            throws SQLException {
        String sql =
                "INSERT INTO resources "
                + "(department_id, resource_name, resource_type, "
                + "total_units, deployed_units) "
                + "SELECT department_id, '" + resourceName + "', '"
                + resourceType + "', " + totalUnits + ", 0 "
                + "FROM departments WHERE department_name = '"
                + deptName + "'";
        this.runUpdate(sql);
    } // end method insertResourceByDeptName

    /**
     * Returns true if the named table currently has zero rows.
     *
     * @param tableName the table to check
     * @return true if empty, false otherwise
     * @throws SQLException if the count query fails
     */
    private boolean isTableEmpty(String tableName) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return true;
        }
    } // end method isTableEmpty

    /**
     * Executes a single update/insert statement against the DRS database.
     *
     * @param sql the SQL statement to run
     * @throws SQLException if execution fails
     */
    private void runUpdate(String sql) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    } // end method runUpdate

} // end class DatabaseInitialiser
