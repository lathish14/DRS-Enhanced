package drs.server.net;

import drs.server.db.DatabaseInitialiser;
import drs.server.service.DrsService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

/**
 * Entry point for the DRS server tier. On startup it creates the MySQL schema
 * and tables programmatically (via {@link DatabaseInitialiser}), then opens a
 * {@link ServerSocket} and accepts client connections, handing each one to its
 * own {@link ClientHandler} thread. This thread-per-client model is what makes
 * the server multi-threaded and able to serve many DRS operators at once.
 *
 * <p>Run this class first, then launch one or more JavaFX clients.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DrsServer {

    /** TCP port the server listens on for client connections. */
    public static final int PORT = 5000;

    /** Shared business service used by every client handler. */
    private final DrsService service;

    /** Shared alert broadcaster used for Feature 2 push notifications. */
    private final AlertBroadcaster broadcaster;

    /** Whether the accept loop should keep running. */
    private volatile boolean running;

    /**
     * Constructs the server, wiring up the shared service and broadcaster.
     */
    public DrsServer() {
        this.service = new DrsService();
        this.broadcaster = new AlertBroadcaster();
        this.running = false;
    } // end constructor

    /**
     * Initialises the database, then runs the accept loop until stopped. Each
     * accepted socket is wrapped and handed to a new daemon handler thread.
     *
     * @throws SQLException if database initialisation fails
     */
    public void start() throws SQLException {
        // Programmatic database + table creation and seeding.
        new DatabaseInitialiser().initialise();

        this.running = true;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("========================================");
            System.out.println(" DRS-Enhanced Server listening on port "
                    + PORT);
            System.out.println(" Waiting for client connections...");
            System.out.println("========================================");

            while (this.running) {
                Socket socket = serverSocket.accept();
                this.spawnHandler(socket);
            }
        } catch (IOException ex) {
            System.err.println("[Server] Fatal socket error: "
                    + ex.getMessage());
        }
    } // end method start

    /**
     * Wraps an accepted socket and starts a daemon thread to handle it. A
     * failure to build the connection (e.g. a client that drops immediately)
     * is logged but never stops the server.
     *
     * @param socket the freshly accepted client socket
     */
    private void spawnHandler(Socket socket) {
        try {
            ClientConnection connection = new ClientConnection(socket);
            ClientHandler handler = new ClientHandler(
                    connection, this.service, this.broadcaster);
            Thread thread = new Thread(handler);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException ex) {
            System.err.println("[Server] Failed to set up client: "
                    + ex.getMessage());
        }
    } // end method spawnHandler

    /**
     * Signals the accept loop to stop after the current iteration.
     */
    public void stop() {
        this.running = false;
    } // end method stop

    /**
     * Application entry point. Starts the server and reports any fatal startup
     * error (most commonly a MySQL connection problem) with guidance.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            new DrsServer().start();
        } catch (SQLException ex) {
            System.err.println("======================================");
            System.err.println(" DRS Server failed to start.");
            System.err.println(" Database error: " + ex.getMessage());
            System.err.println(" Check that MySQL is running and that the");
            System.err.println(" credentials in DatabaseConfig are correct.");
            System.err.println("======================================");
        }
    } // end method main

} // end class DrsServer
