package drs.common;

import java.io.Serializable;

/**
 * Represents a user account in the Disaster Response System (DRS).
 * Users are assigned one of four roles that determine their access level
 * and the operations they may perform within the system.
 *
 * <p>This is a shared transfer object. After a successful login the server
 * sends a copy of the authenticated User (with the password field cleared)
 * to the client so the client can apply role-based UI rules.</p>
 *
 * <p>Role hierarchy (highest to lowest):
 * ADMIN, COORDINATOR, DEPARTMENT_STAFF, PUBLIC</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class User implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Class (static) constants - roles ---

    /** Role constant for general public users (disaster reporting only). */
    public static final String ROLE_PUBLIC = "PUBLIC";

    /** Role constant for emergency coordinators (assessment + dashboard). */
    public static final String ROLE_COORDINATOR = "COORDINATOR";

    /** Role constant for department staff (response status updates). */
    public static final String ROLE_DEPARTMENT_STAFF = "DEPARTMENT_STAFF";

    /** Role constant for system administrators (full access). */
    public static final String ROLE_ADMIN = "ADMIN";

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int userId;

    /** Full legal name of the user. */
    private String fullName;

    /** Login username used for authentication. */
    private String username;

    /** Password used for authentication (cleared before sending to client). */
    private String password;

    /** Role assigned to this user, determining access permissions. */
    private String role;

    /** The department or organisation this user belongs to. */
    private String department;

    /** Whether this user account is currently active and can log in. */
    private boolean active;

    // --- Constructors ---

    /**
     * Constructs a new User account with all required details. The account
     * is active by default and the database will assign the real ID on insert.
     *
     * @param fullName   the user's full name
     * @param username   the login username (must be unique)
     * @param password   the login password
     * @param role       the role assigned to the user
     * @param department the department or organisation the user belongs to
     */
    public User(String fullName,
                String username,
                String password,
                String role,
                String department) {
        this.userId = 0;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.department = department;
        this.active = true;
    } // end constructor

    /**
     * Constructs a fully-populated User, typically when reconstructing an
     * object from a MySQL result set on the server side.
     *
     * @param userId     the database-assigned unique ID
     * @param fullName   the user's full name
     * @param username   the login username
     * @param password   the stored password
     * @param role       the user's role
     * @param department the user's department
     * @param active     whether the account is active
     */
    public User(int userId,
                String fullName,
                String username,
                String password,
                String role,
                String department,
                boolean active) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.department = department;
        this.active = active;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique database-assigned user ID.
     *
     * @return the integer user ID
     */
    public int getUserId() {
        return this.userId;
    } // end method getUserId

    /**
     * Returns the user's full name.
     *
     * @return the full name string
     */
    public String getFullName() {
        return this.fullName;
    } // end method getFullName

    /**
     * Returns the login username.
     *
     * @return the username string
     */
    public String getUsername() {
        return this.username;
    } // end method getUsername

    /**
     * Returns the stored password for this user.
     *
     * @return the password string
     */
    public String getPassword() {
        return this.password;
    } // end method getPassword

    /**
     * Returns the role assigned to this user.
     *
     * @return one of ROLE_ADMIN, ROLE_COORDINATOR,
     *         ROLE_DEPARTMENT_STAFF, or ROLE_PUBLIC
     */
    public String getRole() {
        return this.role;
    } // end method getRole

    /**
     * Returns the department or organisation this user belongs to.
     *
     * @return the department name string
     */
    public String getDepartment() {
        return this.department;
    } // end method getDepartment

    /**
     * Returns whether this user account is currently active.
     *
     * @return true if the account is active, false if deactivated
     */
    public boolean isActive() {
        return this.active;
    } // end method isActive

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID.
     *
     * @param userId the database ID to assign
     */
    public void setUserId(int userId) {
        this.userId = userId;
    } // end method setUserId

    /**
     * Sets the user's full name.
     *
     * @param fullName the new full name to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    } // end method setFullName

    /**
     * Sets the login username.
     *
     * @param username the new username to set
     */
    public void setUsername(String username) {
        this.username = username;
    } // end method setUsername

    /**
     * Sets the login password.
     *
     * @param password the new password to set
     */
    public void setPassword(String password) {
        this.password = password;
    } // end method setPassword

    /**
     * Sets the role assigned to this user.
     *
     * @param role the role string to set
     */
    public void setRole(String role) {
        this.role = role;
    } // end method setRole

    /**
     * Sets the department or organisation of this user.
     *
     * @param department the department name to set
     */
    public void setDepartment(String department) {
        this.department = department;
    } // end method setDepartment

    /**
     * Sets whether this user account is active.
     *
     * @param active true to activate the account, false to deactivate
     */
    public void setActive(boolean active) {
        this.active = active;
    } // end method setActive

    // --- Other methods ---

    /**
     * Returns a formatted string representation of this User object,
     * suitable for display in the admin user management view.
     *
     * @return formatted multi-field string with user details
     */
    @Override
    public String toString() {
        return "User #" + this.userId
                + " | Name: " + this.fullName
                + " | Username: " + this.username
                + " | Role: " + this.role
                + " | Department: " + this.department
                + " | Active: " + this.active;
    } // end method toString

} // end class User
