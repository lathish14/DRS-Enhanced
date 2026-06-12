package drs.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable statistics snapshot computed on the server from the current
 * disaster dataset and serialized to the client for the Statistics dashboard.
 *
 * <p>In the Assessment Two version statistics were computed live on the client
 * from an in-memory list. In the DRS-Enhanced three-tier design the server
 * owns the data, so it computes the figures (with SQL aggregate queries) and
 * ships this compact snapshot to the client instead of the whole dataset.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Stats implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Instance variables ---

    /** Total number of disasters recorded. */
    private int totalCount;

    /** Number of HIGH priority disasters. */
    private int highPriorityCount;

    /** Number of MEDIUM priority disasters. */
    private int mediumPriorityCount;

    /** Number of disasters with status RESOLVED. */
    private int resolvedCount;

    /** Number of active (unresolved) disasters. */
    private int activeCount;

    /** Arithmetic mean severity score across all disasters. */
    private double averageSeverity;

    /** Count of disasters grouped by disaster type. */
    private Map<String, Integer> countByType;

    /** Count of disasters grouped by priority level. */
    private Map<String, Integer> countByPriority;

    /** Total resource units deployed across all active dispatches. */
    private int totalUnitsDeployed;

    // --- Constructor ---

    /**
     * Constructs an empty Stats snapshot with zeroed counters and empty maps.
     * Fields are populated by the server via the setters below.
     */
    public Stats() {
        this.totalCount = 0;
        this.highPriorityCount = 0;
        this.mediumPriorityCount = 0;
        this.resolvedCount = 0;
        this.activeCount = 0;
        this.averageSeverity = 0.0;
        this.countByType = new LinkedHashMap<>();
        this.countByPriority = new HashMap<>();
        this.totalUnitsDeployed = 0;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the total disaster count.
     *
     * @return total count
     */
    public int getTotalCount() {
        return this.totalCount;
    } // end method getTotalCount

    /**
     * Returns the HIGH priority disaster count.
     *
     * @return high priority count
     */
    public int getHighPriorityCount() {
        return this.highPriorityCount;
    } // end method getHighPriorityCount

    /**
     * Returns the MEDIUM priority disaster count.
     *
     * @return medium priority count
     */
    public int getMediumPriorityCount() {
        return this.mediumPriorityCount;
    } // end method getMediumPriorityCount

    /**
     * Returns the resolved disaster count.
     *
     * @return resolved count
     */
    public int getResolvedCount() {
        return this.resolvedCount;
    } // end method getResolvedCount

    /**
     * Returns the active (unresolved) disaster count.
     *
     * @return active count
     */
    public int getActiveCount() {
        return this.activeCount;
    } // end method getActiveCount

    /**
     * Returns the average severity score.
     *
     * @return average severity as a double
     */
    public double getAverageSeverity() {
        return this.averageSeverity;
    } // end method getAverageSeverity

    /**
     * Returns the map of disaster type to count.
     *
     * @return count-by-type map
     */
    public Map<String, Integer> getCountByType() {
        return this.countByType;
    } // end method getCountByType

    /**
     * Returns the map of priority level to count.
     *
     * @return count-by-priority map
     */
    public Map<String, Integer> getCountByPriority() {
        return this.countByPriority;
    } // end method getCountByPriority

    /**
     * Returns the total resource units currently deployed.
     *
     * @return total deployed units
     */
    public int getTotalUnitsDeployed() {
        return this.totalUnitsDeployed;
    } // end method getTotalUnitsDeployed

    // --- Mutator (set) methods ---

    /**
     * Sets the total disaster count.
     *
     * @param totalCount the value to set
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    } // end method setTotalCount

    /**
     * Sets the HIGH priority count.
     *
     * @param highPriorityCount the value to set
     */
    public void setHighPriorityCount(int highPriorityCount) {
        this.highPriorityCount = highPriorityCount;
    } // end method setHighPriorityCount

    /**
     * Sets the MEDIUM priority count.
     *
     * @param mediumPriorityCount the value to set
     */
    public void setMediumPriorityCount(int mediumPriorityCount) {
        this.mediumPriorityCount = mediumPriorityCount;
    } // end method setMediumPriorityCount

    /**
     * Sets the resolved count.
     *
     * @param resolvedCount the value to set
     */
    public void setResolvedCount(int resolvedCount) {
        this.resolvedCount = resolvedCount;
    } // end method setResolvedCount

    /**
     * Sets the active count.
     *
     * @param activeCount the value to set
     */
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    } // end method setActiveCount

    /**
     * Sets the average severity.
     *
     * @param averageSeverity the value to set
     */
    public void setAverageSeverity(double averageSeverity) {
        this.averageSeverity = averageSeverity;
    } // end method setAverageSeverity

    /**
     * Sets the count-by-type map.
     *
     * @param countByType the map to set
     */
    public void setCountByType(Map<String, Integer> countByType) {
        this.countByType = countByType;
    } // end method setCountByType

    /**
     * Sets the count-by-priority map.
     *
     * @param countByPriority the map to set
     */
    public void setCountByPriority(Map<String, Integer> countByPriority) {
        this.countByPriority = countByPriority;
    } // end method setCountByPriority

    /**
     * Sets the total deployed units figure.
     *
     * @param totalUnitsDeployed the value to set
     */
    public void setTotalUnitsDeployed(int totalUnitsDeployed) {
        this.totalUnitsDeployed = totalUnitsDeployed;
    } // end method setTotalUnitsDeployed

    // --- Other methods ---

    /**
     * Returns a concise one-line summary of this statistics snapshot.
     *
     * @return formatted summary string
     */
    @Override
    public String toString() {
        return "Stats"
                + " | Total: " + this.totalCount
                + " | Active: " + this.activeCount
                + " | High: " + this.highPriorityCount
                + " | Resolved: " + this.resolvedCount
                + " | Avg Severity: "
                + String.format("%.1f", this.averageSeverity)
                + " | Units Deployed: " + this.totalUnitsDeployed;
    } // end method toString

} // end class Stats
