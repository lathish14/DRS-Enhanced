package drs.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single timestamped entry in the Disaster Response System audit
 * log. Every significant action performed in the DRS (disaster reporting,
 * assessment, department notification, status update, or resource dispatch)
 * generates a ResponseLog entry, providing a complete and traceable activity
 * history for each disaster.
 *
 * <p>This is a shared transfer object. Log entries are written to MySQL on the
 * server and serialized to the client for display in the audit log view.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class ResponseLog implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Class (static) constants - action types ---

    /** Action type for disaster report submission entries. */
    public static final String ACTION_REPORTED = "DISASTER_REPORTED";

    /** Action type for severity assessment completion entries. */
    public static final String ACTION_ASSESSED = "ASSESSMENT_DONE";

    /** Action type for department notification entries. */
    public static final String ACTION_DEPT_NOTIFIED = "DEPT_NOTIFIED";

    /** Action type for response status change entries. */
    public static final String ACTION_STATUS_UPDATE = "STATUS_UPDATE";

    /** Action type for resource dispatch/allocation entries. */
    public static final String ACTION_RESOURCE_DISPATCH = "RESOURCE_DISPATCH";

    /** Action type for general notes or observations. */
    public static final String ACTION_NOTE = "NOTE";

    /** Date-time format pattern for log timestamps. */
    public static final String LOG_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

    // --- Instance variables - Rule 16: noun names ---

    /** Unique identifier assigned by the database (AUTO_INCREMENT). */
    private int logId;

    /** The ID of the disaster this log entry belongs to. */
    private int disasterId;

    /** The username of the user who performed this action. */
    private String actionBy;

    /** A human-readable description of the action that was performed. */
    private String actionDescription;

    /** The category/type of the action (see ACTION_* constants). */
    private String actionType;

    /** The precise date and time when this action was performed. */
    private LocalDateTime logTime;

    // --- Constructors ---

    /**
     * Constructs a new ResponseLog entry for a freshly performed action.
     * The timestamp is set to now and the database assigns the real ID.
     *
     * @param disasterId        the ID of the related disaster
     * @param actionBy          the username of the acting user
     * @param actionDescription a description of the action performed
     * @param actionType        the action category (use ACTION_* constants)
     */
    public ResponseLog(int disasterId,
                       String actionBy,
                       String actionDescription,
                       String actionType) {
        this.logId = 0;
        this.disasterId = disasterId;
        this.actionBy = actionBy;
        this.actionDescription = actionDescription;
        this.actionType = actionType;
        this.logTime = LocalDateTime.now();
    } // end constructor

    /**
     * Constructs a fully-populated ResponseLog, typically when reconstructing
     * an object from a MySQL result set on the server side.
     *
     * @param logId             the database-assigned unique ID
     * @param disasterId        the related disaster ID
     * @param actionBy          the acting username
     * @param actionDescription the action description
     * @param actionType        the action category
     * @param logTime           the timestamp of the action
     */
    public ResponseLog(int logId,
                       int disasterId,
                       String actionBy,
                       String actionDescription,
                       String actionType,
                       LocalDateTime logTime) {
        this.logId = logId;
        this.disasterId = disasterId;
        this.actionBy = actionBy;
        this.actionDescription = actionDescription;
        this.actionType = actionType;
        this.logTime = logTime;
    } // end constructor

    // --- Accessor (get) methods ---

    /**
     * Returns the unique database-assigned log entry ID.
     *
     * @return the integer log ID
     */
    public int getLogId() {
        return this.logId;
    } // end method getLogId

    /**
     * Returns the ID of the disaster this log entry belongs to.
     *
     * @return the disaster ID integer
     */
    public int getDisasterId() {
        return this.disasterId;
    } // end method getDisasterId

    /**
     * Returns the username of the user who performed this action.
     *
     * @return the actionBy username string
     */
    public String getActionBy() {
        return this.actionBy;
    } // end method getActionBy

    /**
     * Returns the descriptive text of the action performed.
     *
     * @return the action description string
     */
    public String getActionDescription() {
        return this.actionDescription;
    } // end method getActionDescription

    /**
     * Returns the category/type of the action.
     *
     * @return the action type string (one of the ACTION_* constants)
     */
    public String getActionType() {
        return this.actionType;
    } // end method getActionType

    /**
     * Returns the raw LocalDateTime when this log entry was created.
     *
     * @return the log timestamp as a LocalDateTime object
     */
    public LocalDateTime getLogTime() {
        return this.logTime;
    } // end method getLogTime

    /**
     * Returns a human-readable formatted string of the log timestamp.
     * Format: dd/MM/yyyy HH:mm:ss (includes seconds for precise audit trail).
     *
     * @return formatted date-time string
     */
    public String getLogTimeFormatted() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(LOG_DATE_FORMAT);
        return this.logTime.format(formatter);
    } // end method getLogTimeFormatted

    // --- Mutator (set) methods ---

    /**
     * Sets the database-assigned unique ID.
     *
     * @param logId the database ID to assign
     */
    public void setLogId(int logId) {
        this.logId = logId;
    } // end method setLogId

    /**
     * Sets the action description text.
     *
     * @param actionDescription the new description string to set
     */
    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    } // end method setActionDescription

    /**
     * Sets the action type category.
     *
     * @param actionType the action type string to set
     */
    public void setActionType(String actionType) {
        this.actionType = actionType;
    } // end method setActionType

    // --- Other methods ---

    /**
     * Returns a formatted string representation of this ResponseLog entry,
     * suitable for display in the audit log view of the application.
     *
     * @return formatted multi-field string with all log details
     */
    @Override
    public String toString() {
        return "[" + this.getLogTimeFormatted() + "]"
                + " Log #" + this.logId
                + " | Disaster #" + this.disasterId
                + " | By: " + this.actionBy
                + " | Type: " + this.actionType
                + " | Action: " + this.actionDescription;
    } // end method toString

} // end class ResponseLog
