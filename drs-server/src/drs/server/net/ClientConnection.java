package drs.server.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Wraps a single client's socket and its object streams for the DRS server.
 *
 * <p>Two different server threads may need to write to the same client: the
 * owning {@link ClientHandler} thread (sending a {@code Response} to a request)
 * and the {@link AlertBroadcaster} (pushing a live {@code Alert} triggered by
 * another client's action, for Enhanced Feature 2). Concurrent writes to one
 * {@code ObjectOutputStream} would corrupt the stream, so all sends funnel
 * through the {@code synchronized} {@link #send(Object)} method here.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ClientConnection {

    /** The underlying client socket. */
    private final Socket socket;

    /** Object output stream for sending responses and alerts to the client. */
    private final ObjectOutputStream out;

    /** Object input stream for reading requests from the client. */
    private final ObjectInputStream in;

    /** Username of the authenticated user on this connection (may be null). */
    private volatile String username;

    /**
     * Wraps the given socket, creating its object streams. The output stream
     * is created and flushed first so the peer's input stream header can be
     * read without a deadlock.
     *
     * @param socket the accepted client socket
     * @throws IOException if the streams cannot be created
     */
    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.username = null;
    } // end constructor

    /**
     * Reads the next object sent by the client. Blocks until one arrives.
     *
     * @return the next object from the client
     * @throws IOException            if a stream error occurs
     * @throws ClassNotFoundException if the object's class is unknown
     */
    public Object receive() throws IOException, ClassNotFoundException {
        return this.in.readObject();
    } // end method receive

    /**
     * Sends an object to the client. Synchronized so that response writes and
     * asynchronous alert pushes never interleave on the stream. The stream is
     * reset after each write so updated object state is always re-serialised.
     *
     * @param payload the object to send (a Response or an Alert)
     * @throws IOException if the write fails
     */
    public synchronized void send(Object payload) throws IOException {
        this.out.writeObject(payload);
        this.out.flush();
        this.out.reset();
    } // end method send

    /**
     * Returns the username associated with this connection, if logged in.
     *
     * @return the username, or null
     */
    public String getUsername() {
        return this.username;
    } // end method getUsername

    /**
     * Associates an authenticated username with this connection.
     *
     * @param username the username to record
     */
    public void setUsername(String username) {
        this.username = username;
    } // end method setUsername

    /**
     * Closes the socket and its streams, swallowing any error.
     */
    public void close() {
        try {
            this.in.close();
        } catch (IOException ignored) {
            // best effort
        }
        try {
            this.out.close();
        } catch (IOException ignored) {
            // best effort
        }
        try {
            this.socket.close();
        } catch (IOException ignored) {
            // best effort
        }
    } // end method close

    /**
     * Returns the remote address of this client for logging.
     *
     * @return the remote socket address as a string
     */
    public String getRemoteAddress() {
        return String.valueOf(this.socket.getRemoteSocketAddress());
    } // end method getRemoteAddress

} // end class ClientConnection
