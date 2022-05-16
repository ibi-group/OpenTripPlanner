package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.FeedScopedId;

public class TestSpecificTransfer {

  /**
   * Test different specific transfers
   */
  @Test
  public void testSpecificTransfer() {
    // Setup from trip with route
    Route fromRoute = new Route(TransitModelForTest.id("R1"));
    Trip fromTrip = new Trip(TransitModelForTest.id("T1"));
    fromTrip.setRoute(fromRoute);

    // Setup to trip with route
    Route toRoute = new Route(TransitModelForTest.id("R2"));
    Trip toTrip = new Trip(TransitModelForTest.id("T2"));
    toTrip.setRoute(toRoute);

    // Create full SpecificTransfer
    SpecificTransfer s1 = new SpecificTransfer(
      fromRoute.getId(),
      toRoute.getId(),
      fromTrip.getId(),
      toTrip.getId(),
      1
    );
    assertTrue(s1.matches(fromTrip, toTrip));
    assertEquals(SpecificTransfer.MAX_SPECIFICITY, s1.getSpecificity());
    assertEquals(1, s1.transferTime);

    // Create empty SpecificTransfer
    SpecificTransfer s2 = new SpecificTransfer((FeedScopedId) null, null, null, null, 2);
    assertTrue(s2.matches(fromTrip, toTrip));
    assertEquals(SpecificTransfer.MIN_SPECIFICITY, s2.getSpecificity());
    assertEquals(2, s2.transferTime);

    // Create SpecificTransfer one trip missing
    SpecificTransfer s3 = new SpecificTransfer(
      fromRoute.getId(),
      toRoute.getId(),
      null,
      toTrip.getId(),
      3
    );
    assertTrue(s3.matches(fromTrip, toTrip));
    assertEquals(3, s3.getSpecificity());
    assertEquals(3, s3.transferTime);

    // Create SpecificTransfer one trip different
    SpecificTransfer s4 = new SpecificTransfer(
      fromRoute.getId(),
      toRoute.getId(),
      TransitModelForTest.id("T3"),
      toTrip.getId(),
      4
    );
    assertFalse(s4.matches(fromTrip, toTrip));
    assertEquals(SpecificTransfer.MAX_SPECIFICITY, s4.getSpecificity());
    assertEquals(4, s4.transferTime);

    // Create SpecificTransfer one trip and route missing
    SpecificTransfer s5 = new SpecificTransfer(null, toRoute.getId(), null, toTrip.getId(), 5);
    assertTrue(s5.matches(fromTrip, toTrip));
    assertEquals(2, s5.getSpecificity());
    assertEquals(5, s5.transferTime);

    // Create SpecificTransfer one trip only
    SpecificTransfer s6 = new SpecificTransfer(null, null, null, toTrip.getId(), 6);
    assertTrue(s6.matches(fromTrip, toTrip));
    assertEquals(2, s6.getSpecificity());
    assertEquals(6, s6.transferTime);

    // Create SpecificTransfer one route only
    SpecificTransfer s7 = new SpecificTransfer(fromRoute.getId(), null, null, null, 7);
    assertTrue(s7.matches(fromTrip, toTrip));
    assertEquals(1, s7.getSpecificity());
    assertEquals(7, s7.transferTime);
  }
}
