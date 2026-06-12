package drs.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JUnit 4 test class for the {@link Disaster} shared model.
 * Covers constructor behaviour, accessor and mutator methods, priority
 * derivation logic (including boundary values 3/4 and 7/8), default status,
 * and the toString() output.
 *
 * <p>In the DRS-Enhanced design the disaster ID is assigned by the MySQL
 * database, so a newly constructed Disaster has ID 0 until persisted. Tests
 * here therefore focus on in-memory logic rather than ID generation.</p>
 *
 * <p>Test IDs use the format TC-D-NN and correspond to the Test Plan in the
 * group report.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class DisasterTest {

    /** The Disaster instance shared across tests in this class. */
    private Disaster disaster;

    /**
     * Creates a fresh Disaster before each test for isolation.
     */
    @Before
    public void setUp() {
        this.disaster = new Disaster(
                "Fire",
                "123 George Street, Sydney",
                "Large warehouse fire with reported casualties",
                "Jane Citizen");
    } // end method setUp

    /**
     * Releases the reference after each test.
     */
    @After
    public void tearDown() {
        this.disaster = null;
    } // end method tearDown

    /**
     * TC-D-01: A new disaster has ID 0 until the database assigns one.
     */
    @Test
    public void testNewDisasterHasZeroId() {
        assertEquals("New disaster ID should be 0 before persistence",
                0, this.disaster.getDisasterId());
    } // end test testNewDisasterHasZeroId

    /**
     * TC-D-02: Constructor stores the disaster type.
     */
    @Test
    public void testConstructorStoresType() {
        assertEquals("Fire", this.disaster.getDisasterType());
    } // end test testConstructorStoresType

    /**
     * TC-D-03: Constructor stores the location.
     */
    @Test
    public void testConstructorStoresLocation() {
        assertEquals("123 George Street, Sydney",
                this.disaster.getLocation());
    } // end test testConstructorStoresLocation

    /**
     * TC-D-04: A new disaster defaults to REPORTED status.
     */
    @Test
    public void testDefaultStatusIsReported() {
        assertEquals(Disaster.STATUS_REPORTED, this.disaster.getStatus());
    } // end test testDefaultStatusIsReported

    /**
     * TC-D-05: A new disaster defaults to LOW priority (severity 0).
     */
    @Test
    public void testDefaultPriorityIsLow() {
        assertEquals(Disaster.PRIORITY_LOW,
                this.disaster.getPriorityLevel());
    } // end test testDefaultPriorityIsLow

    /**
     * TC-D-06: Severity 8 derives HIGH priority (lower HIGH boundary).
     */
    @Test
    public void testSeverityEightIsHigh() {
        this.disaster.setSeverityScore(8);
        assertEquals(Disaster.PRIORITY_HIGH,
                this.disaster.getPriorityLevel());
    } // end test testSeverityEightIsHigh

    /**
     * TC-D-07: Severity 10 derives HIGH priority (upper boundary).
     */
    @Test
    public void testSeverityTenIsHigh() {
        this.disaster.setSeverityScore(10);
        assertEquals(Disaster.PRIORITY_HIGH,
                this.disaster.getPriorityLevel());
    } // end test testSeverityTenIsHigh

    /**
     * TC-D-08: Severity 7 derives MEDIUM priority (upper MEDIUM boundary).
     */
    @Test
    public void testSeveritySevenIsMedium() {
        this.disaster.setSeverityScore(7);
        assertEquals(Disaster.PRIORITY_MEDIUM,
                this.disaster.getPriorityLevel());
    } // end test testSeveritySevenIsMedium

    /**
     * TC-D-09: Severity 4 derives MEDIUM priority (lower MEDIUM boundary).
     */
    @Test
    public void testSeverityFourIsMedium() {
        this.disaster.setSeverityScore(4);
        assertEquals(Disaster.PRIORITY_MEDIUM,
                this.disaster.getPriorityLevel());
    } // end test testSeverityFourIsMedium

    /**
     * TC-D-10: Severity 3 derives LOW priority (upper LOW boundary).
     */
    @Test
    public void testSeverityThreeIsLow() {
        this.disaster.setSeverityScore(3);
        assertEquals(Disaster.PRIORITY_LOW,
                this.disaster.getPriorityLevel());
    } // end test testSeverityThreeIsLow

    /**
     * TC-D-11: Severity 1 derives LOW priority (lower boundary).
     */
    @Test
    public void testSeverityOneIsLow() {
        this.disaster.setSeverityScore(1);
        assertEquals(Disaster.PRIORITY_LOW,
                this.disaster.getPriorityLevel());
    } // end test testSeverityOneIsLow

    /**
     * TC-D-12: Setting status is reflected by the accessor.
     */
    @Test
    public void testSetStatus() {
        this.disaster.setStatus(Disaster.STATUS_RESOLVED);
        assertEquals(Disaster.STATUS_RESOLVED, this.disaster.getStatus());
    } // end test testSetStatus

    /**
     * TC-D-13: The database-assigned ID can be set and read back.
     */
    @Test
    public void testSetDisasterId() {
        this.disaster.setDisasterId(42);
        assertEquals(42, this.disaster.getDisasterId());
    } // end test testSetDisasterId

    /**
     * TC-D-14: The full (from-database) constructor derives priority too.
     */
    @Test
    public void testFullConstructorDerivesPriority() {
        Disaster fromDb = new Disaster(5, "Flood", "Townsville",
                "River flooding", 9, Disaster.STATUS_RESPONDING,
                java.time.LocalDateTime.now(), "Operator", "Hospital");
        assertEquals(Disaster.PRIORITY_HIGH, fromDb.getPriorityLevel());
    } // end test testFullConstructorDerivesPriority

    /**
     * TC-D-15: toString() includes the type and location.
     */
    @Test
    public void testToStringContainsKeyFields() {
        String text = this.disaster.toString();
        assertNotNull(text);
        assertTrue("toString should contain the type",
                text.contains("Fire"));
        assertTrue("toString should contain the severity label",
                text.contains("Severity"));
    } // end test testToStringContainsKeyFields

    /**
     * TC-D-16: The formatted timestamp is non-null and reasonably sized.
     */
    @Test
    public void testReportedAtFormatted() {
        String formatted = this.disaster.getReportedAtFormatted();
        assertNotNull(formatted);
        assertTrue("Formatted date should be at least 10 chars",
                formatted.length() >= 10);
    } // end test testReportedAtFormatted

} // end class DisasterTest
