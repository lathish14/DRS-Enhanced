package drs.common;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JUnit 4 test class covering the {@link Dispatch} model (Enhanced Feature 1)
 * and the {@link Request}/{@link Response} protocol envelopes that the client
 * and server exchange over the network.
 *
 * <p>Test IDs use the format TC-P-NN and correspond to the Test Plan in the
 * group report.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class DispatchProtocolTest {

    /**
     * TC-P-01: A new dispatch defaults to DEPLOYED status.
     */
    @Test
    public void testNewDispatchDefaultsDeployed() {
        Dispatch dispatch = new Dispatch(
                1, 2, "Fire Truck", "Fire & Emergency", 3, "coordinator");
        assertEquals(Dispatch.STATUS_DEPLOYED, dispatch.getStatus());
    } // end test testNewDispatchDefaultsDeployed

    /**
     * TC-P-02: A new dispatch records the unit count and authoriser.
     */
    @Test
    public void testNewDispatchStoresFields() {
        Dispatch dispatch = new Dispatch(
                1, 2, "Fire Truck", "Fire & Emergency", 3, "coordinator");
        assertEquals(3, dispatch.getUnitsDispatched());
        assertEquals("coordinator", dispatch.getDispatchedBy());
        assertEquals(1, dispatch.getDisasterId());
    } // end test testNewDispatchStoresFields

    /**
     * TC-P-03: Dispatch status can be changed to RECALLED.
     */
    @Test
    public void testDispatchStatusChange() {
        Dispatch dispatch = new Dispatch(
                1, 2, "Fire Truck", "Fire & Emergency", 3, "coordinator");
        dispatch.setStatus(Dispatch.STATUS_RECALLED);
        assertEquals(Dispatch.STATUS_RECALLED, dispatch.getStatus());
    } // end test testDispatchStatusChange

    /**
     * TC-P-04: Request stores its action name.
     */
    @Test
    public void testRequestAction() {
        Request request = new Request(Request.ACTION_LOGIN);
        assertEquals(Request.ACTION_LOGIN, request.getAction());
    } // end test testRequestAction

    /**
     * TC-P-05: Request put/getString round-trips a value, with fluent return.
     */
    @Test
    public void testRequestStringParam() {
        Request request = new Request(Request.ACTION_LOGIN)
                .put("username", "admin")
                .put("password", "secret");
        assertEquals("admin", request.getString("username"));
        assertEquals("secret", request.getString("password"));
    } // end test testRequestStringParam

    /**
     * TC-P-06: Request integer parameters round-trip and parse back.
     */
    @Test
    public void testRequestIntParam() {
        Request request = new Request(Request.ACTION_DISPATCH)
                .put("disasterId", 5)
                .put("units", 3);
        assertEquals(5, request.getInt("disasterId", -1));
        assertEquals(3, request.getInt("units", -1));
    } // end test testRequestIntParam

    /**
     * TC-P-07: Request.getInt returns the default for a missing key.
     */
    @Test
    public void testRequestIntDefault() {
        Request request = new Request(Request.ACTION_GET_STATS);
        assertEquals(99, request.getInt("absent", 99));
    } // end test testRequestIntDefault

    /**
     * TC-P-08: Response.ok produces a success response carrying a payload.
     */
    @Test
    public void testResponseOk() {
        Response response = Response.ok("done", "payload");
        assertTrue(response.isSuccess());
        assertEquals("done", response.getMessage());
        assertEquals("payload", response.getPayload());
    } // end test testResponseOk

    /**
     * TC-P-09: Response.fail produces a failure response with no payload.
     */
    @Test
    public void testResponseFail() {
        Response response = Response.fail("nope");
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
    } // end test testResponseFail

    /**
     * TC-P-10: Response.asList returns an empty list when payload is not a
     * list, protecting callers from ClassCastException.
     */
    @Test
    public void testResponseAsListEmptyWhenNoList() {
        Response response = Response.ok("none", "not-a-list");
        assertNotNull(response.asList());
        assertTrue(response.asList().isEmpty());
    } // end test testResponseAsListEmptyWhenNoList

    /**
     * TC-P-11: Response.asUser returns the User payload when present.
     */
    @Test
    public void testResponseAsUser() {
        User user = new User("Admin", "admin", "x",
                User.ROLE_ADMIN, "Administration");
        Response response = Response.ok("ok", user);
        assertNotNull(response.asUser());
        assertEquals("admin", response.asUser().getUsername());
    } // end test testResponseAsUser

} // end class DispatchProtocolTest
