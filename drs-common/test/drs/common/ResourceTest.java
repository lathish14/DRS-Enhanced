package drs.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JUnit 4 test class for the {@link Resource} shared model, which underpins
 * Enhanced Feature 1 (Resource &amp; Dispatch Management). Focuses on the
 * derived availability calculation and its boundary behaviour.
 *
 * <p>Test IDs use the format TC-R-NN and correspond to the Test Plan in the
 * group report.</p>
 *
 * @author DRS Team
 * @version 2.0 (DRS-Enhanced)
 */
public class ResourceTest {

    /** The Resource instance shared across tests. */
    private Resource resource;

    /**
     * Creates a fresh resource (10 total, 0 deployed) before each test.
     */
    @Before
    public void setUp() {
        this.resource = new Resource(
                1, "Fire & Emergency", "Fire Truck", "Vehicle", 10);
    } // end method setUp

    /**
     * Releases the reference after each test.
     */
    @After
    public void tearDown() {
        this.resource = null;
    } // end method tearDown

    /**
     * TC-R-01: A new resource starts with zero deployed units.
     */
    @Test
    public void testNewResourceZeroDeployed() {
        assertEquals(0, this.resource.getDeployedUnits());
    } // end test testNewResourceZeroDeployed

    /**
     * TC-R-02: Availability equals total when nothing is deployed.
     */
    @Test
    public void testFullAvailabilityWhenNoneDeployed() {
        assertEquals(10, this.resource.getAvailableUnits());
    } // end test testFullAvailabilityWhenNoneDeployed

    /**
     * TC-R-03: Availability is total minus deployed.
     */
    @Test
    public void testAvailabilityAfterDeployment() {
        this.resource.setDeployedUnits(4);
        assertEquals(6, this.resource.getAvailableUnits());
    } // end test testAvailabilityAfterDeployment

    /**
     * TC-R-04: Availability is zero when all units are deployed.
     */
    @Test
    public void testZeroAvailabilityWhenAllDeployed() {
        this.resource.setDeployedUnits(10);
        assertEquals(0, this.resource.getAvailableUnits());
    } // end test testZeroAvailabilityWhenAllDeployed

    /**
     * TC-R-05: Availability never goes negative even if deployed exceeds
     * total (defensive clamp).
     */
    @Test
    public void testAvailabilityNeverNegative() {
        this.resource.setDeployedUnits(15);
        assertTrue("Available units must not be negative",
                this.resource.getAvailableUnits() >= 0);
        assertEquals(0, this.resource.getAvailableUnits());
    } // end test testAvailabilityNeverNegative

    /**
     * TC-R-06: The owning department name is stored and returned.
     */
    @Test
    public void testDepartmentName() {
        assertEquals("Fire & Emergency",
                this.resource.getDepartmentName());
    } // end test testDepartmentName

    /**
     * TC-R-07: The database-assigned ID can be set and read back.
     */
    @Test
    public void testSetResourceId() {
        this.resource.setResourceId(7);
        assertEquals(7, this.resource.getResourceId());
    } // end test testSetResourceId

    /**
     * TC-R-08: toString() reflects the available/total figures.
     */
    @Test
    public void testToStringShowsAvailability() {
        this.resource.setDeployedUnits(3);
        String text = this.resource.toString();
        assertTrue(text.contains("Fire Truck"));
        assertTrue(text.contains("7"));  // available 10-3
    } // end test testToStringShowsAvailability

} // end class ResourceTest
