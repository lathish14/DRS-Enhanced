package drs.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A request envelope sent from the JavaFX client to the multi-threaded server
 * over an object stream. Each request carries an {@code action} string naming
 * the operation to perform and a map of named parameters.
 *
 * <p>This forms the client-to-server half of the DRS application protocol.
 * The server dispatches on {@link #getAction()} and reads typed parameters
 * via the convenience getters below.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Request implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Action name constants ---

    /** Authenticate a user. Params: username, password. */
    public static final String ACTION_LOGIN = "LOGIN";

    /** Submit a new disaster report. Params: type, location, desc, by. */
    public static final String ACTION_REPORT_DISASTER = "REPORT_DISASTER";

    /** Retrieve all disasters. No params. */
    public static final String ACTION_GET_DISASTERS = "GET_DISASTERS";

    /** Assess a disaster. Params: disasterId, severity. */
    public static final String ACTION_ASSESS = "ASSESS_DISASTER";

    /** Notify departments for a disaster. Params: disasterId. */
    public static final String ACTION_NOTIFY = "NOTIFY_DEPARTMENTS";

    /** Update a disaster status. Params: disasterId, status. */
    public static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";

    /** Retrieve all departments. No params. */
    public static final String ACTION_GET_DEPARTMENTS = "GET_DEPARTMENTS";

    /** Retrieve all audit logs. No params. */
    public static final String ACTION_GET_LOGS = "GET_LOGS";

    /** Retrieve all users (admin). No params. */
    public static final String ACTION_GET_USERS = "GET_USERS";

    /** Add a new user (admin). Params: fullName, username, etc. */
    public static final String ACTION_ADD_USER = "ADD_USER";

    /** Toggle a user's active flag (admin). Params: userId, active. */
    public static final String ACTION_SET_USER_ACTIVE = "SET_USER_ACTIVE";

    /** Get computed statistics snapshot. No params. */
    public static final String ACTION_GET_STATS = "GET_STATS";

    // --- Feature 1: resource & dispatch actions ---

    /** Retrieve all resources. No params. */
    public static final String ACTION_GET_RESOURCES = "GET_RESOURCES";

    /** Dispatch resource units to a disaster.
     *  Params: disasterId, resourceId, units. */
    public static final String ACTION_DISPATCH = "DISPATCH_RESOURCE";

    /** Retrieve dispatches for a disaster. Params: disasterId. */
    public static final String ACTION_GET_DISPATCHES = "GET_DISPATCHES";

    /** Recall a dispatch (free its units). Params: dispatchId. */
    public static final String ACTION_RECALL = "RECALL_DISPATCH";

    // --- Feature 2: real-time alert subscription ---

    /** Register this connection to receive live alert broadcasts. No params. */
    public static final String ACTION_SUBSCRIBE_ALERTS = "SUBSCRIBE_ALERTS";

    /** Politely disconnect the session. No params. */
    public static final String ACTION_LOGOUT = "LOGOUT";

    /**
     * Self-register a new account. Params: fullName, username, password,
     * role, department. No session required (called before login).
     */
    public static final String ACTION_REGISTER = "REGISTER";

    // --- Instance variables ---

    /** The action name identifying the requested operation. */
    private String action;

    /** Named parameters for the request. Values are stored as Strings. */
    private Map<String, String> params;

    /** The username of the session making this request (may be null). */
    private String sessionUser;

    // --- Constructor ---

    /**
     * Constructs a new Request for the given action with an empty parameter
     * map.
     *
     * @param action the action name (use one of the ACTION_* constants)
     */
    public Request(String action) {
        this.action = action;
        this.params = new HashMap<>();
        this.sessionUser = null;
    } // end constructor

    // --- Accessor / mutator methods ---

    /**
     * Returns the action name for this request.
     *
     * @return the action string
     */
    public String getAction() {
        return this.action;
    } // end method getAction

    /**
     * Returns the username of the session that issued this request.
     *
     * @return the session username, or null if not set
     */
    public String getSessionUser() {
        return this.sessionUser;
    } // end method getSessionUser

    /**
     * Sets the username of the session issuing this request.
     *
     * @param sessionUser the session username
     */
    public void setSessionUser(String sessionUser) {
        this.sessionUser = sessionUser;
    } // end method setSessionUser

    /**
     * Adds or replaces a string parameter and returns this Request, enabling
     * fluent chaining such as {@code new Request(A).put("k", v).put(...)}.
     *
     * @param key   the parameter name
     * @param value the parameter value
     * @return this Request instance
     */
    public Request put(String key, String value) {
        this.params.put(key, value);
        return this;
    } // end method put

    /**
     * Adds or replaces an integer parameter (stored as its string form).
     *
     * @param key   the parameter name
     * @param value the integer value
     * @return this Request instance
     */
    public Request put(String key, int value) {
        this.params.put(key, String.valueOf(value));
        return this;
    } // end method put

    /**
     * Returns the raw string value of a parameter.
     *
     * @param key the parameter name
     * @return the string value, or null if absent
     */
    public String getString(String key) {
        return this.params.get(key);
    } // end method getString

    /**
     * Returns the integer value of a parameter, or a default if absent or
     * unparseable.
     *
     * @param key          the parameter name
     * @param defaultValue the value to return if missing/invalid
     * @return the parsed integer, or defaultValue
     */
    public int getInt(String key, int defaultValue) {
        String raw = this.params.get(key);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    } // end method getInt

    // --- Other methods ---

    /**
     * Returns a concise string representation of this request for logging.
     *
     * @return formatted request string
     */
    @Override
    public String toString() {
        return "Request{action=" + this.action
                + ", user=" + this.sessionUser
                + ", params=" + this.params + "}";
    } // end method toString

} // end class Request
