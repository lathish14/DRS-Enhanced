package drs.server.net;

import drs.common.Alert;
import drs.common.Disaster;
import drs.common.Dispatch;
import drs.common.Request;
import drs.common.Response;
import drs.common.User;
import drs.server.service.DrsService;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 * Handles all communication with a single connected client on its own thread,
 * giving the DRS server its multi-threaded, multi-user capability. Each handler
 * reads {@link Request} objects in a loop, dispatches them to the shared
 * {@link DrsService}, and writes back a {@link Response}.
 *
 * <p>After certain successful operations the handler also constructs an
 * {@link Alert} and asks the {@link AlertBroadcaster} to push it to every
 * connected client, implementing Enhanced Feature 2.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ClientHandler implements Runnable {

    /** This client's connection wrapper. */
    private final ClientConnection connection;

    /** Shared business service (one instance used by all handlers). */
    private final DrsService service;

    /** Shared alert broadcaster for Feature 2. */
    private final AlertBroadcaster broadcaster;

    /**
     * Constructs a handler for one client.
     *
     * @param connection  the client connection
     * @param service     the shared business service
     * @param broadcaster the shared alert broadcaster
     */
    public ClientHandler(ClientConnection connection,
                         DrsService service,
                         AlertBroadcaster broadcaster) {
        this.connection = connection;
        this.service = service;
        this.broadcaster = broadcaster;
    } // end constructor

    /**
     * The thread body: repeatedly read a request, process it, and send a
     * response until the client disconnects or logs out.
     */
    @Override
    public void run() {
        System.out.println("[Server] Client connected: "
                + this.connection.getRemoteAddress());
        try {
            boolean running = true;
            while (running) {
                Object received = this.connection.receive();
                if (!(received instanceof Request)) {
                    continue;
                }
                Request request = (Request) received;
                if (Request.ACTION_LOGOUT.equals(request.getAction())) {
                    this.connection.send(Response.ok("Goodbye."));
                    running = false;
                } else {
                    Response response = this.process(request);
                    this.connection.send(response);
                }
            }
        } catch (EOFException eof) {
            System.out.println("[Server] Client closed connection: "
                    + this.connection.getRemoteAddress());
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("[Server] Connection error ("
                    + this.connection.getRemoteAddress() + "): "
                    + ex.getMessage());
        } finally {
            this.broadcaster.unsubscribe(this.connection);
            this.connection.close();
            System.out.println("[Server] Handler ended for "
                    + this.connection.getRemoteAddress());
        }
    } // end method run

    /**
     * Routes a single request to the correct service operation and builds the
     * response. Any exception is converted into a failure response so one bad
     * request never kills the client thread.
     *
     * @param request the incoming request
     * @return the response to send back
     */
    private Response process(Request request) {
        try {
            switch (request.getAction()) {
                case Request.ACTION_LOGIN:
                    return this.handleLogin(request);
                case Request.ACTION_REPORT_DISASTER:
                    return this.handleReport(request);
                case Request.ACTION_GET_DISASTERS:
                    return Response.ok("OK", this.service.getAllDisasters());
                case Request.ACTION_ASSESS:
                    return this.handleAssess(request);
                case Request.ACTION_NOTIFY:
                    return this.handleNotify(request);
                case Request.ACTION_UPDATE_STATUS:
                    return this.handleUpdateStatus(request);
                case Request.ACTION_GET_DEPARTMENTS:
                    return Response.ok("OK",
                            this.service.getAllDepartments());
                case Request.ACTION_GET_LOGS:
                    return Response.ok("OK", this.service.getAllLogs());
                case Request.ACTION_GET_USERS:
                    return Response.ok("OK", this.service.getAllUsers());
                case Request.ACTION_ADD_USER:
                    return this.handleAddUser(request);
                case Request.ACTION_REGISTER:
                    return this.handleRegister(request);
                case Request.ACTION_SET_USER_ACTIVE:
                    return this.handleSetUserActive(request);
                case Request.ACTION_GET_STATS:
                    return Response.ok("OK", this.service.getStatistics());
                case Request.ACTION_GET_RESOURCES:
                    return Response.ok("OK", this.service.getAllResources());
                case Request.ACTION_DISPATCH:
                    return this.handleDispatch(request);
                case Request.ACTION_GET_DISPATCHES:
                    return Response.ok("OK",
                            this.service.getDispatchesForDisaster(
                                    request.getInt("disasterId", 0)));
                case Request.ACTION_RECALL:
                    return this.handleRecall(request);
                case Request.ACTION_SUBSCRIBE_ALERTS:
                    this.broadcaster.subscribe(this.connection);
                    return Response.ok("Subscribed to live alerts.");
                default:
                    return Response.fail(
                            "Unknown action: " + request.getAction());
            }
        } catch (IllegalArgumentException ex) {
            return Response.fail(ex.getMessage());
        } catch (Exception ex) {
            System.err.println("[Server] Error processing "
                    + request.getAction() + ": " + ex.getMessage());
            return Response.fail("Server error: " + ex.getMessage());
        }
    } // end method process

    /**
     * Authenticates a user and records the username on this connection.
     *
     * @param request the login request
     * @return a response carrying the User on success
     * @throws Exception if the lookup fails
     */
    private Response handleLogin(Request request) throws Exception {
        User user = this.service.login(
                request.getString("username"),
                request.getString("password"));
        if (user == null) {
            return Response.fail("Invalid credentials or inactive account.");
        }
        this.connection.setUsername(user.getUsername());
        return Response.ok("Login successful.", user);
    } // end method handleLogin

    /**
     * Stores a new disaster report and broadcasts an informational alert.
     *
     * @param request the report request
     * @return a response carrying the stored Disaster
     * @throws Exception if persistence fails
     */
    private Response handleReport(Request request) throws Exception {
        Disaster disaster = this.service.reportDisaster(
                request.getString("type"),
                request.getString("location"),
                request.getString("description"),
                request.getString("reportedBy"),
                request.getSessionUser());
        this.broadcaster.broadcast(new Alert(
                Alert.LEVEL_INFO,
                "New Disaster Reported",
                disaster.getDisasterType() + " at "
                        + disaster.getLocation(),
                disaster.getDisasterId(),
                safeUser(request)));
        return Response.ok("Disaster reported (ID "
                + disaster.getDisasterId() + ").", disaster);
    } // end method handleReport

    /**
     * Assesses a disaster and, if the result is HIGH priority, broadcasts a
     * critical alert to all clients.
     *
     * @param request the assess request
     * @return success or failure response
     * @throws Exception if persistence fails
     */
    private Response handleAssess(Request request) throws Exception {
        int disasterId = request.getInt("disasterId", 0);
        int severity = request.getInt("severity", 0);
        boolean ok = this.service.assessDisaster(
                disasterId, severity, request.getSessionUser());
        if (!ok) {
            return Response.fail("Disaster #" + disasterId + " not found.");
        }
        if (severity >= 8) {
            this.broadcaster.broadcast(new Alert(
                    Alert.LEVEL_CRITICAL,
                    "HIGH Priority Disaster",
                    "Disaster #" + disasterId
                            + " assessed at severity " + severity
                            + " — immediate response required.",
                    disasterId,
                    safeUser(request)));
        }
        return Response.ok("Assessment recorded.");
    } // end method handleAssess

    /**
     * Notifies departments for a disaster and broadcasts an informational
     * alert listing them.
     *
     * @param request the notify request
     * @return a response carrying the notified department names
     * @throws Exception if persistence fails
     */
    private Response handleNotify(Request request) throws Exception {
        int disasterId = request.getInt("disasterId", 0);
        List<String> notified = this.service.notifyDepartments(
                disasterId, request.getSessionUser());
        this.broadcaster.broadcast(new Alert(
                Alert.LEVEL_INFO,
                "Departments Notified",
                "Disaster #" + disasterId + ": "
                        + String.join(", ", notified),
                disasterId,
                safeUser(request)));
        return Response.ok("Notified: " + String.join(", ", notified),
                notified);
    } // end method handleNotify

    /**
     * Updates a disaster's status.
     *
     * @param request the update request
     * @return success or failure response
     * @throws Exception if persistence fails
     */
    private Response handleUpdateStatus(Request request) throws Exception {
        int disasterId = request.getInt("disasterId", 0);
        boolean ok = this.service.updateStatus(
                disasterId, request.getString("status"),
                request.getSessionUser());
        return ok
                ? Response.ok("Status updated.")
                : Response.fail("Disaster #" + disasterId + " not found.");
    } // end method handleUpdateStatus

    /**
     * Adds a new user account (admin operation).
     *
     * @param request the add-user request
     * @return a response carrying the created User
     * @throws Exception if persistence fails
     */
    private Response handleAddUser(Request request) throws Exception {
        User user = new User(
                request.getString("fullName"),
                request.getString("username"),
                request.getString("password"),
                request.getString("role"),
                request.getString("department"));
        User saved = this.service.addUser(user);
        return Response.ok("User created (ID " + saved.getUserId() + ").",
                saved);
    } // end method handleAddUser

    /**
     * Handles self-registration of a new account. No session is required,
     * so this is callable from the login/register screen without being
     * logged in.
     *
     * @param request the register request
     * @return a response carrying the created User on success
     * @throws Exception if persistence fails
     */
    private Response handleRegister(Request request) throws Exception {
        User user = new User(
                request.getString("fullName"),
                request.getString("username"),
                request.getString("password"),
                request.getString("role"),
                request.getString("department") == null
                        ? "" : request.getString("department"));
        User saved = this.service.register(user);
        return Response.ok("Account created. You can now log in.", saved);
    } // end method handleRegister

    /**
     * Activates or deactivates a user account (admin operation).
     *
     * @param request the set-active request
     * @return success or failure response
     * @throws Exception if persistence fails
     */
    private Response handleSetUserActive(Request request) throws Exception {
        int userId = request.getInt("userId", 0);
        boolean active = Boolean.parseBoolean(request.getString("active"));
        boolean ok = this.service.setUserActive(userId, active);
        return ok
                ? Response.ok("User updated.")
                : Response.fail("User #" + userId + " not found.");
    } // end method handleSetUserActive

    /**
     * Dispatches resource units to a disaster (Feature 1) and broadcasts a
     * critical alert announcing the deployment.
     *
     * @param request the dispatch request
     * @return a response carrying the created Dispatch
     * @throws Exception if the transaction fails
     */
    private Response handleDispatch(Request request) throws Exception {
        Dispatch dispatch = this.service.dispatchResource(
                request.getInt("disasterId", 0),
                request.getInt("resourceId", 0),
                request.getInt("units", 0),
                request.getSessionUser());
        this.broadcaster.broadcast(new Alert(
                Alert.LEVEL_CRITICAL,
                "Resources Dispatched",
                dispatch.getUnitsDispatched() + "x "
                        + dispatch.getResourceName() + " ("
                        + dispatch.getDepartmentName() + ") to disaster #"
                        + dispatch.getDisasterId(),
                dispatch.getDisasterId(),
                safeUser(request)));
        return Response.ok("Dispatched (ID "
                + dispatch.getDispatchId() + ").", dispatch);
    } // end method handleDispatch

    /**
     * Recalls a deployed dispatch (Feature 1), returning its units.
     *
     * @param request the recall request
     * @return success or failure response
     * @throws Exception if the transaction fails
     */
    private Response handleRecall(Request request) throws Exception {
        int dispatchId = request.getInt("dispatchId", 0);
        boolean ok = this.service.recallDispatch(
                dispatchId, request.getSessionUser());
        return ok
                ? Response.ok("Dispatch recalled.")
                : Response.fail("Dispatch #" + dispatchId
                        + " not found or already recalled.");
    } // end method handleRecall

    /**
     * Returns the request's session username, or "system" if none is set.
     *
     * @param request the request to read
     * @return a non-null source label for alerts
     */
    private static String safeUser(Request request) {
        String user = request.getSessionUser();
        return (user == null || user.trim().isEmpty()) ? "system" : user;
    } // end method safeUser

} // end class ClientHandler
