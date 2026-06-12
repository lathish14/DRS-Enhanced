package drs.server.net;

import drs.common.Alert;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the set of client connections subscribed to live alerts and pushes
 * {@link Alert} objects to all of them. This is the server-side engine of
 * Enhanced Feature 2 (Real-time Multi-user Alerts &amp; Status Broadcast).
 *
 * <p>When any client triggers a noteworthy event (a high-priority assessment,
 * a department notification, or a resource dispatch), the handling thread calls
 * {@link #broadcast(Alert)} and every connected, subscribed client receives the
 * alert asynchronously on its socket. The subscriber set is a thread-safe set
 * backed by a {@link ConcurrentHashMap}, and each individual send is
 * synchronized inside {@link ClientConnection#send(Object)}.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class AlertBroadcaster {

    /** Thread-safe set of connections currently subscribed to alerts. */
    private final Set<ClientConnection> subscribers =
            ConcurrentHashMap.newKeySet();

    /**
     * Registers a connection to receive future alert broadcasts.
     *
     * @param connection the client connection to subscribe
     */
    public void subscribe(ClientConnection connection) {
        this.subscribers.add(connection);
        System.out.println("[Alerts] Subscriber added ("
                + connection.getRemoteAddress() + "). Total: "
                + this.subscribers.size());
    } // end method subscribe

    /**
     * Removes a connection from the subscriber set (on logout or disconnect).
     *
     * @param connection the client connection to unsubscribe
     */
    public void unsubscribe(ClientConnection connection) {
        if (this.subscribers.remove(connection)) {
            System.out.println("[Alerts] Subscriber removed ("
                    + connection.getRemoteAddress() + "). Total: "
                    + this.subscribers.size());
        }
    } // end method unsubscribe

    /**
     * Pushes an alert to every subscribed client. Any connection that fails to
     * receive (because it has dropped) is removed from the subscriber set so
     * the registry self-heals.
     *
     * @param alert the alert to broadcast
     */
    public void broadcast(Alert alert) {
        System.out.println("[Alerts] Broadcasting to "
                + this.subscribers.size() + " client(s): " + alert);
        for (ClientConnection connection : this.subscribers) {
            try {
                connection.send(alert);
            } catch (IOException ex) {
                System.err.println("[Alerts] Dropping dead subscriber: "
                        + connection.getRemoteAddress());
                this.subscribers.remove(connection);
            }
        }
    } // end method broadcast

    /**
     * Returns the current number of active subscribers.
     *
     * @return the subscriber count
     */
    public int subscriberCount() {
        return this.subscribers.size();
    } // end method subscriberCount

} // end class AlertBroadcaster
