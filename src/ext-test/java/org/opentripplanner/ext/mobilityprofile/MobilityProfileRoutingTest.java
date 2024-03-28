package org.opentripplanner.ext.mobilityprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

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

  @Test
  void canProRateProfileCosts() {
    Map<MobilityProfile, Float> profileCost = new HashMap<>();
    profileCost.put(MobilityProfile.NONE, 10.0f);
    profileCost.put(MobilityProfile.DEVICE, 100.0f);

    StreetVertex from = new OsmVertex(33.4, -84.5, 101);
    StreetVertex to = new OsmVertex(33.5, -84.6, 102);

    StreetEdge edge = new StreetEdgeBuilder<>()
      .withProfileCosts(profileCost)
      .withFromVertex(from)
      .withToVertex(to)
      .withPermission(StreetTraversalPermission.ALL)
      .withMeterLength(100)
      .buildAndConnect();
    TemporaryPartialStreetEdge tmpEdge = new TemporaryPartialStreetEdgeBuilder()
      .withParentEdge(edge)
      .withFromVertex(from)
      .withToVertex(to)
      .withMeterLength(40)
      .buildAndConnect();

    Map<MobilityProfile, Float> proRatedProfileCost = MobilityProfileRouting.getProRatedProfileCosts(
      tmpEdge
    );
    assertEquals(4.0f, proRatedProfileCost.get(MobilityProfile.NONE), 1e-6);
    assertEquals(40.0f, proRatedProfileCost.get(MobilityProfile.DEVICE), 1e-6);
  }
}
