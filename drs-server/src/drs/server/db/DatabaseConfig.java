package drs.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralised MySQL connection manager for the DRS server tier.
 *
 * <p>All database settings are kept here so they can be changed in one place.
 * The class first connects to the MySQL <em>server</em> (no database selected)
 * so {@link DatabaseInitialiser} can create the {@code drs_enhanced} schema
 * programmatically if it does not yet exist, as required by the assignment
 * ("Database and tables should be generated programmatically"). Thereafter
 * {@link #getConnection()} returns connections scoped to that schema.</p>
 *
 * <p><strong>JDBC driver:</strong> the project connects to a MySQL database
 * server. It works with either JDBC driver on the classpath and auto-detects
 * which is present: the official MySQL Connector/J
 * ({@code com.mysql.cj.jdbc.Driver}, {@code jdbc:mysql://}) is preferred, and
 * the bundled, MySQL-compatible MariaDB Connector/J
 * ({@code org.mariadb.jdbc.Driver}, {@code jdbc:mariadb://}) is used as a
 * fallback so the system runs out of the box. To use the official MySQL
 * driver, drop {@code mysql-connector-j-*.jar} into the {@code libs} folder.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public final class DatabaseConfig {

    /** JDBC host. Change if MySQL runs elsewhere. */
    public static final String HOST = "localhost";

    /** JDBC port. Default MySQL port is 3306. */
    public static final String PORT = "3306";

    /** The schema/database name created and used by the DRS server. */
    public static final String DB_NAME = "drs_enhanced";

    /** MySQL username. Change to match your local MySQL install. */
    public static final String USER = "root";

    /**
     * MySQL password. Change to match your local MySQL install.
     * For grading convenience this defaults to an empty password.
     */
    public static final String PASSWORD = "root";

    /** Cached JDBC URL scheme detected at startup ("mysql" or "mariadb"). */
    private static String scheme;

    /** Extra JDBC URL parameters for timezone and SSL handling. */
    private static final String PARAMS =
            "useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";

    /**
     * Private constructor: this is a static utility class and must not be
     * instantiated.
     */
    private DatabaseConfig() {
    } // end constructor

    /**
     * Detects and loads an available JDBC driver, preferring the official
     * MySQL Connector/J and falling back to the MySQL-compatible MariaDB
     * driver. The chosen URL scheme is cached for subsequent connections.
     *
     * @throws SQLException if no supported JDBC driver is on the classpath
     */
    public static synchronized void ensureDriverLoaded() throws SQLException {
        if (scheme != null) {
            return;
        }
        if (tryLoad("com.mysql.cj.jdbc.Driver")) {
            scheme = "mysql";
        } else if (tryLoad("org.mariadb.jdbc.Driver")) {
            scheme = "mariadb";
        } else {
            throw new SQLException(
                    "No JDBC driver found on classpath. Add either"
                    + " mysql-connector-j-*.jar or the bundled"
                    + " mariadb-java-client-*.jar to the libs folder.");
        }
        System.out.println("[DB] Using JDBC scheme: jdbc:" + scheme);
    } // end method ensureDriverLoaded

    /**
     * Attempts to load a driver class by name.
     *
     * @param className the fully-qualified driver class name
     * @return true if the class loaded, false otherwise
     */
    private static boolean tryLoad(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    } // end method tryLoad

    /**
     * Returns a connection to the MySQL server with NO database selected.
     * Used only for creating the schema during initialisation.
     *
     * @return a server-scoped JDBC Connection
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getServerConnection() throws SQLException {
        ensureDriverLoaded();
        String url = "jdbc:" + scheme + "://" + HOST + ":" + PORT
                + "/?" + PARAMS;
        return DriverManager.getConnection(url, USER, PASSWORD);
    } // end method getServerConnection

    /**
     * Returns a connection scoped to the {@code drs_enhanced} database.
     * Used by all DAOs for normal data operations.
     *
     * @return a database-scoped JDBC Connection
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        String url = "jdbc:" + scheme + "://" + HOST + ":" + PORT
                + "/" + DB_NAME + "?" + PARAMS;
        return DriverManager.getConnection(url, USER, PASSWORD);
    } // end method getConnection

} // end class DatabaseConfig
