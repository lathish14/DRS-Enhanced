package drs.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A response envelope returned by the multi-threaded server to the JavaFX
 * client. A response carries a success flag, a human-readable message, and an
 * optional payload object (for example a {@link User}, a list of
 * {@link Disaster} objects, or a {@link Stats} snapshot).
 *
 * <p>This forms the server-to-client half of the DRS application protocol.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class Response implements Serializable {

    /** Serialization version identifier for safe network transfer. */
    private static final long serialVersionUID = 1L;

    // --- Instance variables ---

    /** Whether the requested operation succeeded. */
    private boolean success;

    /** A human-readable result or error message. */
    private String message;

    /** The optional payload object accompanying this response. */
    private Object payload;

    // --- Constructors ---

    /**
     * Constructs a Response with no payload.
     *
     * @param success whether the operation succeeded
     * @param message the result or error message
     */
    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.payload = null;
    } // end constructor

    /**
     * Constructs a Response with a payload object.
     *
     * @param success whether the operation succeeded
     * @param message the result or error message
     * @param payload the payload object to return
     */
    public Response(boolean success, String message, Object payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
    } // end constructor

    // --- Static factory helpers ---

    /**
     * Builds a successful response with a payload.
     *
     * @param message the success message
     * @param payload the payload object
     * @return a new success Response
     */
    public static Response ok(String message, Object payload) {
        return new Response(true, message, payload);
    } // end method ok

    /**
     * Builds a successful response with no payload.
     *
     * @param message the success message
     * @return a new success Response
     */
    public static Response ok(String message) {
        return new Response(true, message);
    } // end method ok

    /**
     * Builds a failure response.
     *
     * @param message the error message
     * @return a new failure Response
     */
    public static Response fail(String message) {
        return new Response(false, message);
    } // end method fail

    // --- Accessor methods ---

    /**
     * Returns whether the operation succeeded.
     *
     * @return true on success, false on failure
     */
    public boolean isSuccess() {
        return this.success;
    } // end method isSuccess

    /**
     * Returns the result or error message.
     *
     * @return the message string
     */
    public String getMessage() {
        return this.message;
    } // end method getMessage

    /**
     * Returns the raw payload object.
     *
     * @return the payload, or null if none
     */
    public Object getPayload() {
        return this.payload;
    } // end method getPayload

    /**
     * Returns the payload cast to a {@link User}, or null if the payload is
     * not a User.
     *
     * @return the payload as a User, or null
     */
    public User asUser() {
        return (this.payload instanceof User)
                ? (User) this.payload : null;
    } // end method asUser

    /**
     * Returns the payload cast to a {@link Stats}, or null if not a Stats.
     *
     * @return the payload as a Stats, or null
     */
    public Stats asStats() {
        return (this.payload instanceof Stats)
                ? (Stats) this.payload : null;
    } // end method asStats

    /**
     * Returns the payload as a typed list. If the payload is not a list, an
     * empty list is returned. The unchecked cast is safe in practice because
     * the server always populates these lists with the matching element type
     * named by the corresponding action.
     *
     * @param <T> the expected element type
     * @return the payload as a List, or an empty list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> asList() {
        if (this.payload instanceof List) {
            return (List<T>) this.payload;
        }
        return new ArrayList<>();
    } // end method asList

    // --- Other methods ---

    /**
     * Returns a concise string representation of this response for logging.
     *
     * @return formatted response string
     */
    @Override
    public String toString() {
        return "Response{success=" + this.success
                + ", message=" + this.message
                + ", payload="
                + (this.payload == null
                    ? "null"
                    : this.payload.getClass().getSimpleName())
                + "}";
    } // end method toString

} // end class Response
