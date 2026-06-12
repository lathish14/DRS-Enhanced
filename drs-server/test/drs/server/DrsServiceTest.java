package drs.server;

import drs.common.Disaster;
import drs.common.Dispatch;
import drs.common.Resource;
import drs.common.User;
import drs.server.db.DatabaseConfig;
import drs.server.db.DatabaseInitialiser;
import drs.server.service.DrsService;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JUnit 4 integration tests for the {@link DrsService} business layer running
 * against a live MySQL database. These tests exercise the database-backed
 * logic, including Enhanced Feature 1 (the transactional dispatch and recall
 * of resources).
 *
 * <p>If no MySQL server is reachable with the configured credentials, every
 * test is skipped (via JUnit {@link Assume}) rather than failed, so the build
 * stays green on machines without a database. To run these tests fully, start
 * MySQL with the credentials in {@link DatabaseConfig}.</p>
 *
 * <p>Test IDs use the format TC-S-NN and correspond to the Test Plan in the
 * group report.</p>
 *
 * @author DRS Team (Server tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class DrsServiceTest {

    /** Whether a database connection is available for this test run. */
    private static boolean dbAvailable;

    /** The service under test. */
    private DrsService service;

    /**
     * Probes the database once and initialises the schema if reachable.
     */
    @BeforeClass
    public static void probeDatabase() {
        try (Connection conn = DatabaseConfig.getServerConnection()) {
            new DatabaseInitialiser().initialise();
            dbAvailable = true;
        } catch (Exception ex) {
            dbAvailable = false;
            System.out.println("[DrsServiceTest] Skipping DB tests - "
                    + "no MySQL server reachable: " + ex.getMessage());
        }
    } // end method probeDatabase

    /**
     * Skips the test if the database is unavailable; otherwise builds a fresh
     * service instance.
     */
    @Before
    public void setUp() {
        Assume.assumeTrue("MySQL server not available - skipping",
                dbAvailable);
        this.service = new DrsService();
    } // end method setUp

    /**
     * TC-S-01: Valid credentials authenticate and the password is cleared.
     */
    @Test
    public void testLoginSuccess() throws Exception {
        User user = this.service.login("admin", "admin123");
        assertNotNull("admin should authenticate", user);
        assertTrue("password should be cleared",
                user.getPassword().isEmpty());
    } // end test testLoginSuccess

    /**
     * TC-S-02: Invalid credentials are rejected.
     */
    @Test
    public void testLoginFailure() throws Exception {
        assertEquals(null, this.service.login("admin", "wrong"));
    } // end test testLoginFailure

    /**
     * TC-S-03: A reported disaster receives a database-assigned ID.
     */
    @Test
    public void testReportAssignsId() throws Exception {
        Disaster d = this.service.reportDisaster("Fire", "Cairns",
                "Test fire", "Tester", "tester");
        assertTrue("disaster should get a positive DB id",
                d.getDisasterId() > 0);
    } // end test testReportAssignsId

    /**
     * TC-S-04: Reporting with a blank type is rejected.
     */
    @Test
    public void testReportValidation() {
        try {
            this.service.reportDisaster("", "Cairns", "x", "Tester", "t");
            fail("blank type should be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        } catch (Exception other) {
            fail("unexpected exception: " + other);
        }
    } // end test testReportValidation

    /**
     * TC-S-05: Assessing with severity 9 derives HIGH priority.
     */
    @Test
    public void testAssessDerivesHigh() throws Exception {
        Disaster d = this.service.reportDisaster("Flood", "Lismore",
                "Rising water", "Tester", "tester");
        boolean ok = this.service.assessDisaster(d.getDisasterId(), 9, "co");
        assertTrue(ok);
        Disaster reloaded = findById(this.service.getAllDisasters(),
                d.getDisasterId());
        assertEquals(Disaster.PRIORITY_HIGH, reloaded.getPriorityLevel());
    } // end test testAssessDerivesHigh

    /**
     * TC-S-06: Severity outside 1-10 is rejected.
     */
    @Test
    public void testAssessRejectsOutOfRange() throws Exception {
        Disaster d = this.service.reportDisaster("Storm", "Mackay",
                "Wind", "Tester", "tester");
        try {
            this.service.assessDisaster(d.getDisasterId(), 99, "co");
            fail("severity 99 should be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    } // end test testAssessRejectsOutOfRange

    /**
     * TC-S-07: Dispatching units decreases availability (Feature 1).
     */
    @Test
    public void testDispatchDecreasesAvailability() throws Exception {
        Disaster d = this.service.reportDisaster("Fire", "Toowoomba",
                "Bushfire", "Tester", "tester");
        Resource res = this.service.getAllResources().get(0);
        int before = res.getAvailableUnits();
        Assume.assumeTrue("need available units", before >= 1);

        this.service.dispatchResource(
                d.getDisasterId(), res.getResourceId(), 1, "co");
        Resource after = findResource(this.service.getAllResources(),
                res.getResourceId());
        assertEquals(before - 1, after.getAvailableUnits());
    } // end test testDispatchDecreasesAvailability

    /**
     * TC-S-08: Over-dispatch is rejected and leaves counts unchanged
     * (transaction rollback, Feature 1).
     */
    @Test
    public void testOverDispatchRejected() throws Exception {
        Disaster d = this.service.reportDisaster("Fire", "Bundaberg",
                "Grassfire", "Tester", "tester");
        Resource res = this.service.getAllResources().get(0);
        int before = res.getAvailableUnits();
        try {
            this.service.dispatchResource(
                    d.getDisasterId(), res.getResourceId(),
                    before + 1000, "co");
            fail("over-dispatch should be rejected");
        } catch (IllegalArgumentException expected) {
            Resource after = findResource(this.service.getAllResources(),
                    res.getResourceId());
            assertEquals("availability must be unchanged after rollback",
                    before, after.getAvailableUnits());
        }
    } // end test testOverDispatchRejected

    /**
     * TC-S-09: Recalling a dispatch returns its units (Feature 1).
     */
    @Test
    public void testRecallReturnsUnits() throws Exception {
        Disaster d = this.service.reportDisaster("Fire", "Rockhampton",
                "Structure fire", "Tester", "tester");
        Resource res = this.service.getAllResources().get(0);
        int before = res.getAvailableUnits();
        Assume.assumeTrue("need available units", before >= 2);

        Dispatch dispatch = this.service.dispatchResource(
                d.getDisasterId(), res.getResourceId(), 2, "co");
        this.service.recallDispatch(dispatch.getDispatchId(), "co");
        Resource after = findResource(this.service.getAllResources(),
                res.getResourceId());
        assertEquals(before, after.getAvailableUnits());
    } // end test testRecallReturnsUnits

    /**
     * TC-S-10: Statistics report at least the disasters created here.
     */
    @Test
    public void testStatistics() throws Exception {
        this.service.reportDisaster("Fire", "Gladstone",
                "Test", "Tester", "tester");
        assertTrue(this.service.getStatistics().getTotalCount() >= 1);
    } // end test testStatistics

    // --- helpers ---

    /**
     * Finds a disaster by ID in a list.
     *
     * @param list the list to search
     * @param id   the disaster ID
     * @return the matching disaster, or null
     */
    private static Disaster findById(List<Disaster> list, int id) {
        return list.stream()
                .filter(d -> d.getDisasterId() == id)
                .findFirst().orElse(null);
    } // end method findById

    /**
     * Finds a resource by ID in a list.
     *
     * @param list the list to search
     * @param id   the resource ID
     * @return the matching resource, or null
     */
    private static Resource findResource(List<Resource> list, int id) {
        return list.stream()
                .filter(r -> r.getResourceId() == id)
                .findFirst().orElse(null);
    } // end method findResource

} // end class DrsServiceTest
