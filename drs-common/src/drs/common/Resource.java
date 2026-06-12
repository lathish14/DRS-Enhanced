package drs.common;

import java.io.Serializable;

/**
 * Represents a deployable emergency resource owned by a department in the
 * Disaster Response System (DRS). Examples include fire trucks, ambulances,
 * rescue teams, generators, and water tankers.
 *
 * <p>This class supports <strong>Enhanced Feature 1: Resource &amp; Dispatch
 * Management</strong>. Each resource tracks how many units exist in total and
 * how many are currently deployed to active disasters; the number available
 * is derived from those two values.</p>
 *
 * <p>This is a shared transfer object serialized between server and client.
 * IDs are assigned by the MySQL database (AUTO_INCREMENT).</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Resource implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int resourceId;

    /** The ID of the department that owns this resource. */
    private int departmentId;

    /** Display name of the owning department (denormalised for the GUI). */
    private String departmentName;

    /** Human-readable resource name (e.g., "Fire Truck", "Ambulance"). */
    private String resourceName;

    /** Resource category/type (e.g., "Vehicle", "Personnel", "Equipment"). */
    private String resourceType;

    /** Total number of units of this resource that exist. */
    private int totalUnits;

    /** Number of units currently deployed to active disasters. */
    private int deployedUnits;

    // --- Constructors ---

    /**
     * Constructs a new Resource definition. The database assigns the real ID
     * on insert. Deployed units start at zero.
     *
     * @param departmentId   the owning department's ID
     * @param departmentName the owning department's display name
     * @param resourceName   the resource name
     * @param resourceType   the resource category
     * @param totalUnits     the total number of units available
     */
    public Resource(int departmentId,
                    String departmentName,
                    String resourceName,
                    String resourceType,
                    int totalUnits) {
        this.resourceId = 0;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.totalUnits = totalUnits;
        this.deployedUnits = 0;
    } // end constructor

    /**
     * Constructs a fully-populated Resource, typically when reconstructing an
     * object from a MySQL result set on the server side.
     *
     * @param resourceId     the database-assigned unique ID
     * @param departmentId   the owning department's ID
     * @param departmentName the owning department's display name
     * @param resourceName   the resource name
     * @param resourceType   the resource category
     * @param totalUnits     the total number of units
     * @param deployedUnits  the number of units currently deployed
     */
    public Resource(int resourceId,
                    int departmentId,
                    String departmentName,
                    String resourceName,
                    String resourceType,
                    int totalUnits,
                    int deployedUnits) {
        this.resourceId = resourceId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.totalUnits = totalUnits;
        this.deployedUnits = deployedUnits;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique database-assigned resource ID.
     *
     * @return the integer resource ID
     */
    public int getResourceId() {
        return this.resourceId;
    } // end method getResourceId

    /**
     * Returns the ID of the department that owns this resource.
     *
     * @return the owning department ID
     */
    public int getDepartmentId() {
        return this.departmentId;
    } // end method getDepartmentId

    /**
     * Returns the display name of the owning department.
     *
     * @return the department name string
     */
    public String getDepartmentName() {
        return this.departmentName;
    } // end method getDepartmentName

    /**
     * Returns the human-readable resource name.
     *
     * @return the resource name string
     */
    public String getResourceName() {
        return this.resourceName;
    } // end method getResourceName

    /**
     * Returns the resource category/type.
     *
     * @return the resource type string
     */
    public String getResourceType() {
        return this.resourceType;
    } // end method getResourceType

    /**
     * Returns the total number of units of this resource.
     *
     * @return the total units integer
     */
    public int getTotalUnits() {
        return this.totalUnits;
    } // end method getTotalUnits

    /**
     * Returns the number of units currently deployed.
     *
     * @return the deployed units integer
     */
    public int getDeployedUnits() {
        return this.deployedUnits;
    } // end method getDeployedUnits

    /**
     * Returns the number of units currently available for new dispatch.
     * This is a derived value: total units minus deployed units.
     *
     * @return the available units integer (never negative)
     */
    public int getAvailableUnits() {
        int available = this.totalUnits - this.deployedUnits;
        return Math.max(available, 0);
    } // end method getAvailableUnits

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID.
     *
     * @param resourceId the database ID to assign
     */
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    } // end method setResourceId

    /**
     * Sets the owning department's ID.
     *
     * @param departmentId the department ID to set
     */
    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    } // end method setDepartmentId

    /**
     * Sets the owning department's display name.
     *
     * @param departmentName the department name to set
     */
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    } // end method setDepartmentName

    /**
     * Sets the human-readable resource name.
     *
     * @param resourceName the resource name to set
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    } // end method setResourceName

    /**
     * Sets the resource category/type.
     *
     * @param resourceType the resource type to set
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    } // end method setResourceType

    /**
     * Sets the total number of units.
     *
     * @param totalUnits the total units to set
     */
    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
    } // end method setTotalUnits

    /**
     * Sets the number of units currently deployed.
     *
     * @param deployedUnits the deployed units to set
     */
    public void setDeployedUnits(int deployedUnits) {
        this.deployedUnits = deployedUnits;
    } // end method setDeployedUnits

    // --- Other methods ---

    /**
     * Returns a formatted string representation of this Resource object,
     * suitable for display in the resource management view.
     *
     * @return formatted multi-field string with resource details
     */
    @Override
    public String toString() {
        return "Resource #" + this.resourceId
                + " | " + this.resourceName
                + " (" + this.resourceType + ")"
                + " | Dept: " + this.departmentName
                + " | Available: " + this.getAvailableUnits()
                + "/" + this.totalUnits
                + " | Deployed: " + this.deployedUnits;
    } // end method toString

} // end class Resource
