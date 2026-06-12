package drs.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a dispatch record: a quantity of a specific {@link Resource}
 * allocated to a specific {@link Disaster} at a point in time.
 *
 * <p>This class supports <strong>Enhanced Feature 1: Resource &amp; Dispatch
 * Management</strong>. When a coordinator allocates resources to a disaster,
 * a Dispatch row is created and the resource's deployed-unit count increases.
 * When the disaster is resolved, dispatches can be recalled, decreasing the
 * deployed count again.</p>
 *
 * <p>This is a shared transfer object serialized between server and client.
 * IDs are assigned by the MySQL database (AUTO_INCREMENT).</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Dispatch implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Class (static) constants - dispatch statuses ---

    /** Status constant: resource units are deployed to a disaster. */
    public static final String STATUS_DEPLOYED = "DEPLOYED";

    /** Status constant: resource units have been recalled (freed). */
    public static final String STATUS_RECALLED = "RECALLED";

    /** Date-time format pattern for dispatch timestamps. */
    public static final String DISPATCH_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int dispatchId;

    /** The ID of the disaster these units are dispatched to. */
    private int disasterId;

    /** The ID of the resource being dispatched. */
    private int resourceId;

    /** Display name of the resource (denormalised for the GUI). */
    private String resourceName;

    /** Display name of the owning department (denormalised for the GUI). */
    private String departmentName;

    /** The number of units allocated in this dispatch. */
    private int unitsDispatched;

    /** The username of the coordinator who authorised this dispatch. */
    private String dispatchedBy;

    /** Current status of this dispatch (DEPLOYED or RECALLED). */
    private String status;

    /** Timestamp when this dispatch was created. */
    private LocalDateTime dispatchedAt;

    // --- Constructors ---

    /**
     * Constructs a new Dispatch for a freshly authorised allocation. The
     * database assigns the real ID, the timestamp is set to now, and the
     * status defaults to DEPLOYED.
     *
     * @param disasterId      the target disaster ID
     * @param resourceId      the dispatched resource ID
     * @param resourceName    the resource display name
     * @param departmentName  the owning department display name
     * @param unitsDispatched the number of units allocated
     * @param dispatchedBy    the authorising coordinator's username
     */
    public Dispatch(int disasterId,
                    int resourceId,
                    String resourceName,
                    String departmentName,
                    int unitsDispatched,
                    String dispatchedBy) {
        this.dispatchId = 0;
        this.disasterId = disasterId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.departmentName = departmentName;
        this.unitsDispatched = unitsDispatched;
        this.dispatchedBy = dispatchedBy;
        this.status = STATUS_DEPLOYED;
        this.dispatchedAt = LocalDateTime.now();
    } // end constructor

    /**
     * Constructs a fully-populated Dispatch, typically when reconstructing an
     * object from a MySQL result set on the server side.
     *
     * @param dispatchId      the database-assigned unique ID
     * @param disasterId      the target disaster ID
     * @param resourceId      the dispatched resource ID
     * @param resourceName    the resource display name
     * @param departmentName  the owning department display name
     * @param unitsDispatched the number of units allocated
     * @param dispatchedBy    the authorising coordinator's username
     * @param status          the dispatch status
     * @param dispatchedAt    the dispatch timestamp
     */
    public Dispatch(int dispatchId,
                    int disasterId,
                    int resourceId,
                    String resourceName,
                    String departmentName,
                    int unitsDispatched,
                    String dispatchedBy,
                    String status,
                    LocalDateTime dispatchedAt) {
        this.dispatchId = dispatchId;
        this.disasterId = disasterId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.departmentName = departmentName;
        this.unitsDispatched = unitsDispatched;
        this.dispatchedBy = dispatchedBy;
        this.status = status;
        this.dispatchedAt = dispatchedAt;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique database-assigned dispatch ID.
     *
     * @return the integer dispatch ID
     */
    public int getDispatchId() {
        return this.dispatchId;
    } // end method getDispatchId

    /**
     * Returns the target disaster ID.
     *
     * @return the disaster ID integer
     */
    public int getDisasterId() {
        return this.disasterId;
    } // end method getDisasterId

    /**
     * Returns the dispatched resource ID.
     *
     * @return the resource ID integer
     */
    public int getResourceId() {
        return this.resourceId;
    } // end method getResourceId

    /**
     * Returns the resource display name.
     *
     * @return the resource name string
     */
    public String getResourceName() {
        return this.resourceName;
    } // end method getResourceName

    /**
     * Returns the owning department display name.
     *
     * @return the department name string
     */
    public String getDepartmentName() {
        return this.departmentName;
    } // end method getDepartmentName

    /**
     * Returns the number of units allocated in this dispatch.
     *
     * @return the units dispatched integer
     */
    public int getUnitsDispatched() {
        return this.unitsDispatched;
    } // end method getUnitsDispatched

    /**
     * Returns the username of the authorising coordinator.
     *
     * @return the dispatchedBy username string
     */
    public String getDispatchedBy() {
        return this.dispatchedBy;
    } // end method getDispatchedBy

    /**
     * Returns the current dispatch status.
     *
     * @return one of STATUS_DEPLOYED or STATUS_RECALLED
     */
    public String getStatus() {
        return this.status;
    } // end method getStatus

    /**
     * Returns the raw dispatch timestamp.
     *
     * @return the LocalDateTime when this dispatch was created
     */
    public LocalDateTime getDispatchedAt() {
        return this.dispatchedAt;
    } // end method getDispatchedAt

    /**
     * Returns a human-readable formatted string of the dispatch timestamp.
     *
     * @return formatted date-time string
     */
    public String getDispatchedAtFormatted() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(DISPATCH_DATE_FORMAT);
        return this.dispatchedAt.format(formatter);
    } // end method getDispatchedAtFormatted

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID.
     *
     * @param dispatchId the database ID to assign
     */
    public void setDispatchId(int dispatchId) {
        this.dispatchId = dispatchId;
    } // end method setDispatchId

    /**
     * Sets the current dispatch status.
     *
     * @param status the status string to set
     */
    public void setStatus(String status) {
        this.status = status;
    } // end method setStatus

    // --- Other methods ---

    /**
     * Returns a formatted string representation of this Dispatch object,
     * suitable for display in the dispatch history view.
     *
     * @return formatted multi-field string with dispatch details
     */
    @Override
    public String toString() {
        return "Dispatch #" + this.dispatchId
                + " | Disaster #" + this.disasterId
                + " | " + this.unitsDispatched + "x " + this.resourceName
                + " (" + this.departmentName + ")"
                + " | Status: " + this.status
                + " | By: " + this.dispatchedBy
                + " | At: " + this.getDispatchedAtFormatted();
    } // end method toString

} // end class Dispatch
