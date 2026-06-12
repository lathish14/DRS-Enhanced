package drs.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a reported disaster event in the Disaster Response System (DRS).
 * This is a shared transfer object: instances are created on the server (from
 * MySQL rows) and serialized across the network to the JavaFX client, so the
 * class implements {@link Serializable}.
 *
 * <p>Priority level is automatically derived from the severity score whenever
 * the score is set. Unlike the Assessment Two version, the unique disaster ID
 * is assigned by the MySQL database (AUTO_INCREMENT), not by a static counter,
 * so this class no longer owns an in-memory ID generator.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Disaster implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Class (static) constants - priority levels ---

    /** Constant for HIGH priority level (severity score 8-10). */
    public static final String PRIORITY_HIGH = "HIGH";

    /** Constant for MEDIUM priority level (severity score 4-7). */
    public static final String PRIORITY_MEDIUM = "MEDIUM";

    /** Constant for LOW priority level (severity score 1-3). */
    public static final String PRIORITY_LOW = "LOW";

    // --- Class (static) constants - lifecycle statuses ---

    /** Status constant: disaster submitted but not yet assessed. */
    public static final String STATUS_REPORTED = "REPORTED";

    /** Status constant: disaster is currently being assessed. */
    public static final String STATUS_UNDER_ASSESSMENT = "UNDER ASSESSMENT";

    /** Status constant: departments dispatched and actively responding. */
    public static final String STATUS_RESPONDING = "RESPONDING";

    /** Status constant: disaster has been fully resolved. */
    public static final String STATUS_RESOLVED = "RESOLVED";

    /** Date-time format pattern used for displaying the report timestamp. */
    public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int disasterId;

    /** Category of disaster (e.g., Fire, Hurricane, Earthquake). */
    private String disasterType;

    /** Physical location where the disaster occurred. */
    private String location;

    /** Descriptive details about the disaster situation. */
    private String description;

    /** Assessed severity on a scale of 1 (minor) to 10 (catastrophic). */
    private int severityScore;

    /** System-derived priority level: HIGH, MEDIUM, or LOW. */
    private String priorityLevel;

    /** Current lifecycle status of the disaster response. */
    private String status;

    /** Timestamp recording when this disaster report was submitted. */
    private LocalDateTime reportedAt;

    /** Full name of the person who submitted the report. */
    private String reportedBy;

    /** Comma-separated names of all departments notified. */
    private String notifiedDepartments;

    // --- Constructors ---

    /**
     * Constructs a new Disaster for a freshly reported event. The ID is left
     * as 0 (the database will assign the real ID on insert), the timestamp is
     * set to now, initial status is REPORTED, and initial priority is LOW.
     *
     * @param disasterType the category of disaster being reported
     * @param location     the physical address or area of the disaster
     * @param description  a detailed description of the situation
     * @param reportedBy   the full name of the person submitting the report
     */
    public Disaster(String disasterType,
                    String location,
                    String description,
                    String reportedBy) {
        this.disasterId = 0;
        this.disasterType = disasterType;
        this.location = location;
        this.description = description;
        this.reportedBy = reportedBy;
        this.severityScore = 0;
        this.priorityLevel = PRIORITY_LOW;
        this.status = STATUS_REPORTED;
        this.reportedAt = LocalDateTime.now();
        this.notifiedDepartments = "";
    } // end constructor

    /**
     * Constructs a fully-populated Disaster, typically when reconstructing an
     * object from a MySQL result set on the server side.
     *
     * @param disasterId          the database-assigned unique ID
     * @param disasterType        the category of disaster
     * @param location            the physical location
     * @param description         the situation description
     * @param severityScore       the severity score (0 if unassessed)
     * @param status              the current lifecycle status
     * @param reportedAt          the report timestamp
     * @param reportedBy          the reporter's full name
     * @param notifiedDepartments comma-separated notified department names
     */
    public Disaster(int disasterId,
                    String disasterType,
                    String location,
                    String description,
                    int severityScore,
                    String status,
                    LocalDateTime reportedAt,
                    String reportedBy,
                    String notifiedDepartments) {
        this.disasterId = disasterId;
        this.disasterType = disasterType;
        this.location = location;
        this.description = description;
        this.severityScore = severityScore;
        this.status = status;
        this.reportedAt = reportedAt;
        this.reportedBy = reportedBy;
        this.notifiedDepartments =
                (notifiedDepartments == null) ? "" : notifiedDepartments;
        this.updatePriorityFromSeverity();
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique disaster ID assigned by the database.
     *
     * @return the integer disaster ID
     */
    public int getDisasterId() {
        return this.disasterId;
    } // end method getDisasterId

    /**
     * Returns the type/category of this disaster.
     *
     * @return the disaster type string (e.g., "Fire", "Earthquake")
     */
    public String getDisasterType() {
        return this.disasterType;
    } // end method getDisasterType

    /**
     * Returns the physical location of this disaster.
     *
     * @return the location string
     */
    public String getLocation() {
        return this.location;
    } // end method getLocation

    /**
     * Returns the descriptive text for this disaster.
     *
     * @return the description string
     */
    public String getDescription() {
        return this.description;
    } // end method getDescription

    /**
     * Returns the current assessed severity score.
     * Returns 0 if the disaster has not yet been assessed.
     *
     * @return severity score integer in range 0-10
     */
    public int getSeverityScore() {
        return this.severityScore;
    } // end method getSeverityScore

    /**
     * Returns the current priority level derived from the severity score.
     *
     * @return one of PRIORITY_HIGH, PRIORITY_MEDIUM, or PRIORITY_LOW
     */
    public String getPriorityLevel() {
        return this.priorityLevel;
    } // end method getPriorityLevel

    /**
     * Returns the current response lifecycle status of this disaster.
     *
     * @return status string (e.g., STATUS_REPORTED, STATUS_RESPONDING)
     */
    public String getStatus() {
        return this.status;
    } // end method getStatus

    /**
     * Returns the raw LocalDateTime when this disaster was reported.
     *
     * @return the LocalDateTime timestamp object
     */
    public LocalDateTime getReportedAt() {
        return this.reportedAt;
    } // end method getReportedAt

    /**
     * Returns a human-readable formatted string of the report timestamp.
     * Uses the pattern defined in DATE_FORMAT (dd/MM/yyyy HH:mm).
     *
     * @return formatted date-time string
     */
    public String getReportedAtFormatted() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(DATE_FORMAT);
        return this.reportedAt.format(formatter);
    } // end method getReportedAtFormatted

    /**
     * Returns the full name of the person who submitted this report.
     *
     * @return the reporter's full name string
     */
    public String getReportedBy() {
        return this.reportedBy;
    } // end method getReportedBy

    /**
     * Returns the comma-separated list of notified department names.
     * Returns an empty string if no departments have been notified.
     *
     * @return the notified departments string
     */
    public String getNotifiedDepartments() {
        return this.notifiedDepartments;
    } // end method getNotifiedDepartments

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID. Normally called once, immediately
     * after the row is inserted and the generated key is read back.
     *
     * @param disasterId the database ID to assign
     */
    public void setDisasterId(int disasterId) {
        this.disasterId = disasterId;
    } // end method setDisasterId

    /**
     * Sets the type/category of this disaster.
     *
     * @param disasterType the new disaster type string
     */
    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    } // end method setDisasterType

    /**
     * Sets the physical location of this disaster.
     *
     * @param location the new location string
     */
    public void setLocation(String location) {
        this.location = location;
    } // end method setLocation

    /**
     * Sets the description of this disaster.
     *
     * @param description the new description string
     */
    public void setDescription(String description) {
        this.description = description;
    } // end method setDescription

    /**
     * Sets the severity score and automatically updates the priority level.
     * Score thresholds: 8-10 maps to HIGH, 4-7 maps to MEDIUM,
     * 1-3 maps to LOW.
     *
     * @param severityScore the severity score; must be in range 1-10
     */
    public void setSeverityScore(int severityScore) {
        this.severityScore = severityScore;
        this.updatePriorityFromSeverity();
    } // end method setSeverityScore

    /**
     * Directly overrides the priority level string. In normal operation,
     * use setSeverityScore() for automatic derivation.
     *
     * @param priorityLevel the new priority level string to set
     */
    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    } // end method setPriorityLevel

    /**
     * Sets the current lifecycle status of this disaster.
     *
     * @param status the new status string to set
     */
    public void setStatus(String status) {
        this.status = status;
    } // end method setStatus

    /**
     * Sets the name of the person who reported this disaster.
     *
     * @param reportedBy the reporter's full name to set
     */
    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    } // end method setReportedBy

    /**
     * Sets the report timestamp. Used when rebuilding from a database row.
     *
     * @param reportedAt the LocalDateTime to set
     */
    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    } // end method setReportedAt

    /**
     * Sets the comma-separated list of departments notified for this disaster.
     *
     * @param notifiedDepartments the department names string to set
     */
    public void setNotifiedDepartments(String notifiedDepartments) {
        this.notifiedDepartments = notifiedDepartments;
    } // end method setNotifiedDepartments

    // --- Other methods ---

    /**
     * Derives and assigns the priority level from the current severity score.
     * Called automatically by setSeverityScore() and the full constructor.
     */
    public void updatePriorityFromSeverity() {
        if (this.severityScore >= 8) {
            this.priorityLevel = PRIORITY_HIGH;
        } else if (this.severityScore >= 4) {
            this.priorityLevel = PRIORITY_MEDIUM;
        } else {
            this.priorityLevel = PRIORITY_LOW;
        }
    } // end method updatePriorityFromSeverity

    /**
     * Returns a formatted string representation of this Disaster object,
     * suitable for display in list views and system log output.
     *
     * @return formatted multi-field string with disaster details
     */
    @Override
    public String toString() {
        return "Disaster #" + this.disasterId
                + " | Type: " + this.disasterType
                + " | Location: " + this.location
                + " | Severity: " + this.severityScore
                + " | Priority: " + this.priorityLevel
                + " | Status: " + this.status
                + " | Reported: " + this.getReportedAtFormatted()
                + " | By: " + this.reportedBy;
    } // end method toString

} // end class Disaster
