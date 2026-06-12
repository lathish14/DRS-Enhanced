package drs.common;

import java.io.Serializable;

/**
 * Represents an external department or organisation in the Disaster Response
 * System (DRS). Departments receive notifications from DRS based on disaster
 * type and severity, and update their response status as they act.
 *
 * <p>This is a shared transfer object serialized between server and client.
 * IDs are assigned by the MySQL database (AUTO_INCREMENT).</p>
 *
 * <p>The eight standard departments in the DRS are: Fire and Emergency,
 * Hospital, Electricity, Transportation, Waste Management, Water Supply,
 * Law Enforcement, and Schools.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Department implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Class (static) constants - statuses ---

    /** Status constant: department has not yet been contacted. */
    public static final String STATUS_STANDBY = "STANDBY";

    /** Status constant: department has received a DRS notification. */
    public static final String STATUS_NOTIFIED = "NOTIFIED";

    /** Status constant: department is actively responding to a disaster. */
    public static final String STATUS_RESPONDING = "RESPONDING";

    /** Status constant: department has completed its response role. */
    public static final String STATUS_COMPLETED = "COMPLETED";

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int departmentId;

    /** Full official name of the department. */
    private String departmentName;

    /** Primary contact phone number for emergency dispatch. */
    private String contactNumber;

    /** Primary contact email address for the department. */
    private String contactEmail;

    /** The category of disaster this department specialises in handling. */
    private String specialization;

    /** The current operational response status of this department. */
    private String responseStatus;

    // --- Constructors ---

    /**
     * Constructs a new Department with all required contact and operational
     * details. Response status defaults to STANDBY and the database will
     * assign the real ID on insert.
     *
     * @param departmentName the official name of the department
     * @param contactNumber  the emergency contact phone number
     * @param contactEmail   the contact email address
     * @param specialization the disaster category this department handles
     */
    public Department(String departmentName,
                      String contactNumber,
                      String contactEmail,
                      String specialization) {
        this.departmentId = 0;
        this.departmentName = departmentName;
        this.contactNumber = contactNumber;
        this.contactEmail = contactEmail;
        this.specialization = specialization;
        this.responseStatus = STATUS_STANDBY;
    } // end constructor

    /**
     * Constructs a fully-populated Department, typically when reconstructing
     * an object from a MySQL result set on the server side.
     *
     * @param departmentId   the database-assigned unique ID
     * @param departmentName the official department name
     * @param contactNumber  the contact phone number
     * @param contactEmail   the contact email
     * @param specialization the disaster category handled
     * @param responseStatus the current response status
     */
    public Department(int departmentId,
                      String departmentName,
                      String contactNumber,
                      String contactEmail,
                      String specialization,
                      String responseStatus) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.contactNumber = contactNumber;
        this.contactEmail = contactEmail;
        this.specialization = specialization;
        this.responseStatus = responseStatus;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique database-assigned department ID.
     *
     * @return the integer department ID
     */
    public int getDepartmentId() {
        return this.departmentId;
    } // end method getDepartmentId

    /**
     * Returns the official name of this department.
     *
     * @return the department name string
     */
    public String getDepartmentName() {
        return this.departmentName;
    } // end method getDepartmentName

    /**
     * Returns the primary contact phone number.
     *
     * @return the contact number string
     */
    public String getContactNumber() {
        return this.contactNumber;
    } // end method getContactNumber

    /**
     * Returns the primary contact email address.
     *
     * @return the contact email string
     */
    public String getContactEmail() {
        return this.contactEmail;
    } // end method getContactEmail

    /**
     * Returns the disaster category this department specialises in.
     *
     * @return the specialization string
     */
    public String getSpecialization() {
        return this.specialization;
    } // end method getSpecialization

    /**
     * Returns the current operational response status.
     *
     * @return one of STATUS_STANDBY, STATUS_NOTIFIED,
     *         STATUS_RESPONDING, or STATUS_COMPLETED
     */
    public String getResponseStatus() {
        return this.responseStatus;
    } // end method getResponseStatus

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID.
     *
     * @param departmentId the database ID to assign
     */
    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    } // end method setDepartmentId

    /**
     * Sets the official name of this department.
     *
     * @param departmentName the department name to set
     */
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    } // end method setDepartmentName

    /**
     * Sets the contact phone number for this department.
     *
     * @param contactNumber the contact number to set
     */
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    } // end method setContactNumber

    /**
     * Sets the contact email address for this department.
     *
     * @param contactEmail the contact email to set
     */
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    } // end method setContactEmail

    /**
     * Sets the disaster category specialization for this department.
     *
     * @param specialization the specialization string to set
     */
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    } // end method setSpecialization

    /**
     * Sets the current operational response status of this department.
     *
     * @param responseStatus the status string to set
     */
    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    } // end method setResponseStatus

    // --- Other methods ---

    /**
     * Returns a formatted string representation of this Department object,
     * suitable for display in the department coordination view.
     *
     * @return formatted multi-field string with department details
     */
    @Override
    public String toString() {
        return "Department #" + this.departmentId
                + " | Name: " + this.departmentName
                + " | Specialization: " + this.specialization
                + " | Status: " + this.responseStatus
                + " | Contact: " + this.contactNumber
                + " | Email: " + this.contactEmail;
    } // end method toString

} // end class Department
