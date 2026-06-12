package drs.client.net;

import drs.common.Alert;
import drs.common.Request;
import drs.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Client-side networking core for the DRS-Enhanced JavaFX application. Holds a
 * single socket connection to the {@code DrsServer}, sends {@link Request}
 * objects, and receives {@link Response} objects.
 *
 * <p>A dedicated background reader thread consumes every object the server
 * sends. Two kinds of object arrive over the one stream:</p>
 * <ul>
 *   <li>{@link Response} — the reply to a request; handed to the waiting
 *       caller through a blocking queue.</li>
 *   <li>{@link Alert} — an unsolicited live notification pushed by the server
 *       (Enhanced Feature 2); delivered to the registered alert listener so
 *       the GUI can show a banner.</li>
 * </ul>
 *
 * <p>{@link #send(Request)} is synchronized so request/response pairs never
 * interleave, while pushed alerts are routed independently of any pending
 * request.</p>
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ServerConnection {

    /** Default server host. */
    public static final String DEFAULT_HOST = "localhost";

    /** Default server port (must match DrsServer.PORT). */
    public static final int DEFAULT_PORT = 5000;

    /** Single shared instance for the whole client application. */
    private static ServerConnection instance;

    /** The socket connection to the server. */
    private Socket socket;

    /** Stream for sending requests to the server. */
    private ObjectOutputStream out;

    /** Stream for receiving responses and alerts from the server. */
    private ObjectInputStream in;

    /** Queue handing responses from the reader thread to the caller. */
    private final BlockingQueue<Response> responseQueue =
            new LinkedBlockingQueue<>();

    /** Listener notified whenever a live alert is pushed by the server. */
    private Consumer<Alert> alertListener;

    /** The username of the currently logged-in session. */
    private String sessionUser;

    /** Whether the background reader loop should keep running. */
    private volatile boolean connected;

    /**
     * Private constructor enforcing the singleton pattern.
     */
    private ServerConnection() {
        this.connected = false;
    } // end constructor

    /**
     * Returns the shared ServerConnection instance, creating it on first use.
     *
     * @return the singleton ServerConnection
     */
    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    } // end method getInstance

    /**
     * Opens the socket and starts the background reader thread. Safe to call
     * once; subsequent calls while already connected are ignored.
     *
     * @param host the server host name or IP
     * @param port the server port
     * @throws IOException if the connection cannot be established
     */
    public synchronized void connect(String host, int port)
            throws IOException {
        if (this.connected) {
            return;
        }
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(this.socket.getInputStream());
        this.connected = true;

        Thread reader = new Thread(this::readLoop, "drs-server-reader");
        reader.setDaemon(true);
        reader.start();
    } // end method connect

    /**
     * Connects using the default host and port.
     *
     * @throws IOException if the connection cannot be established
     */
    public void connect() throws IOException {
        this.connect(DEFAULT_HOST, DEFAULT_PORT);
    } // end method connect

    /**
     * The background reader loop. Reads each object, routing responses to the
     * response queue and alerts to the alert listener.
     */
    private void readLoop() {
        try {
            while (this.connected) {
                Object obj = this.in.readObject();
                if (obj instanceof Alert) {
                    this.deliverAlert((Alert) obj);
                } else if (obj instanceof Response) {
                    this.responseQueue.offer((Response) obj);
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            if (this.connected) {
                System.err.println("[Client] Connection to server lost: "
                        + ex.getMessage());
            }
        } finally {
            this.connected = false;
        }
    } // end method readLoop

    /**
     * Delivers an alert to the registered listener, guarding against a missing
     * listener and against exceptions thrown by GUI code.
     *
     * @param alert the alert to deliver
     */
    private void deliverAlert(Alert alert) {
        Consumer<Alert> listener = this.alertListener;
        if (listener != null) {
            try {
                listener.accept(alert);
            } catch (RuntimeException ex) {
                System.err.println("[Client] Alert listener error: "
                        + ex.getMessage());
            }
        }
    } // end method deliverAlert

    /**
     * Sends a request and blocks until the matching response arrives. The
     * session username is stamped onto the request automatically. Synchronized
     * so concurrent callers cannot interleave their request/response pairs.
     *
     * @param request the request to send
     * @return the server's response
     * @throws IOException if the connection is down or the write fails
     */
    public synchronized Response send(Request request) throws IOException {
        if (!this.connected) {
            throw new IOException("Not connected to the DRS server.");
        }
        request.setSessionUser(this.sessionUser);
        this.out.writeObject(request);
        this.out.flush();
        this.out.reset();
        try {
            return this.responseQueue.take();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted awaiting server response.", ex);
        }
    } // end method send

    /**
     * Registers (or replaces) the listener that receives pushed alerts.
     *
     * @param listener the alert consumer, or null to clear
     */
    public void setAlertListener(Consumer<Alert> listener) {
        this.alertListener = listener;
    } // end method setAlertListener

    /**
     * Records the logged-in username so it is stamped on subsequent requests.
     *
     * @param sessionUser the username, or null on logout
     */
    public void setSessionUser(String sessionUser) {
        this.sessionUser = sessionUser;
    } // end method setSessionUser

    /**
     * Returns the current session username.
     *
     * @return the username, or null if not logged in
     */
    public String getSessionUser() {
        return this.sessionUser;
    } // end method getSessionUser

    /**
     * Returns whether the client is currently connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return this.connected;
    } // end method isConnected

    /**
     * Closes the connection and stops the reader loop.
     */
    public synchronized void disconnect() {
        this.connected = false;
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ignored) {
            // best effort
        }
    } // end method disconnect

} // end class ServerConnection
