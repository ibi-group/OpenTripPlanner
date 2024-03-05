package org.opentripplanner.ext.mobilityprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.StreetTraversalPermission;

class MobilityProfileRoutingTest {

  @Test
  void canComputeTravelTime() {
    assertEquals(
      0.250 / 1.609 / 2.5,
      MobilityProfileRouting.computeTravelHours(250, MobilityProfile.NONE),
      1e-6
    );
  }

  @Test
  void canDetectHighwayFootwayTag() {
    assertTrue(MobilityProfileRouting.isHighwayFootway(createFootway()));
    assertFalse(MobilityProfileRouting.isHighwayFootway(createServiceWay()));
  }

  private static OSMWay createServiceWay() {
    OSMWay serviceWay = new OSMWay();
    serviceWay.addTag("highway", "service");
    return serviceWay;
  }

  private static OSMWay createFootway() {
    OSMWay footway = new OSMWay();
    footway.addTag("highway", "footway");
    return footway;
  }

  @Test
  void canRemoveWalkPermissionOnNonFootway() {
    OSMWay serviceWay = createServiceWay();
    StreetTraversalPermission permissions = StreetTraversalPermission.ALL;
    assertEquals(
      StreetTraversalPermission.BICYCLE_AND_CAR,
      MobilityProfileRouting.adjustPedestrianPermissions(serviceWay, permissions)
    );
  }

  @Test
  void canPreserveWalkPermissionOnFootway() {
    OSMWay footway = createFootway();
    StreetTraversalPermission permissions = StreetTraversalPermission.ALL;
    assertEquals(
      StreetTraversalPermission.ALL,
      MobilityProfileRouting.adjustPedestrianPermissions(footway, permissions)
    );
  }
}
