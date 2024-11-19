package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class OsmWayTest {

  @Test
  void testIsBicycleDismountForced() {
    OsmWay way = new OsmWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  void testAreaMustContain3Nodes() {
    OsmWay way = new OsmWay();
    way.addTag("area", "yes");
    assertFalse(way.isRoutableArea());
    way.addNodeRef(1);
    assertFalse(way.isRoutableArea());
    way.addNodeRef(2);
    assertFalse(way.isRoutableArea());
    way.addNodeRef(3);
    assertTrue(way.isRoutableArea());
    way.addNodeRef(4);
    assertTrue(way.isRoutableArea());
  }

  @Test
  void testAreaTags() {
    OsmWay platform = getClosedPolygon();
    platform.addTag("public_transport", "platform");
    assertTrue(platform.isRoutableArea());
    platform.addTag("area", "no");
    assertFalse(platform.isRoutableArea());

    OsmWay roundabout = getClosedPolygon();
    roundabout.addTag("highway", "roundabout");
    assertFalse(roundabout.isRoutableArea());

    OsmWay pedestrian = getClosedPolygon();
    pedestrian.addTag("highway", "pedestrian");
    assertFalse(pedestrian.isRoutableArea());
    pedestrian.addTag("area", "yes");
    assertTrue(pedestrian.isRoutableArea());

    OsmWay indoorArea = getClosedPolygon();
    indoorArea.addTag("indoor", "area");
    assertTrue(indoorArea.isRoutableArea());

    OsmWay bikeParking = getClosedPolygon();
    bikeParking.addTag("amenity", "bicycle_parking");
    assertTrue(bikeParking.isRoutableArea());

    OsmWay corridor = getClosedPolygon();
    corridor.addTag("indoor", "corridor");
    assertTrue(corridor.isRoutableArea());

    OsmWay door = getClosedPolygon();
    door.addTag("indoor", "door");
    assertFalse(door.isRoutableArea());
  }

  @Test
  void testIsSteps() {
    OsmWay way = new OsmWay();
    assertFalse(way.isSteps());

    way.addTag("highway", "primary");
    assertFalse(way.isSteps());

    way.addTag("highway", "steps");
    assertTrue(way.isSteps());
  }

  @Test
  void wheelchairAccessibleStairs() {
    var osm1 = new OsmWay();
    osm1.addTag("highway", "steps");
    assertFalse(osm1.isWheelchairAccessible());

    // explicitly suitable for wheelchair users, perhaps because of a ramp
    var osm2 = new OsmWay();
    osm2.addTag("highway", "steps");
    osm2.addTag("wheelchair", "yes");
    assertTrue(osm2.isWheelchairAccessible());
  }

  @Test
  void testIsRoundabout() {
    OsmWay way = new OsmWay();
    assertFalse(way.isRoundabout());

    way.addTag("junction", "dovetail");
    assertFalse(way.isRoundabout());

    way.addTag("junction", "roundabout");
    assertTrue(way.isRoundabout());
  }

  @Test
  void testIsOneWayDriving() {
    OsmWay way = new OsmWay();
    assertFalse(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "notatagvalue");
    assertFalse(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "1");
    assertTrue(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "-1");
    assertFalse(way.isOneWayForwardDriving());
    assertTrue(way.isOneWayReverseDriving());
  }

  @Test
  void testIsOneWayBicycle() {
    OsmWay way = new OsmWay();
    assertFalse(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "notatagvalue");
    assertFalse(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "1");
    assertTrue(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "-1");
    assertFalse(way.isOneWayForwardBicycle());
    assertTrue(way.isOneWayReverseBicycle());
  }

  @Test
  void testIsOpposableCycleway() {
    OsmWay way = new OsmWay();
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "notatagvalue");
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "oppo");
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "opposite");
    assertTrue(way.isOpposableCycleway());

    way.addTag("cycleway", "nope");
    way.addTag("cycleway:left", "opposite_side");
    assertTrue(way.isOpposableCycleway());
  }

  @Test
  void escalator() {
    assertFalse(WayTestData.cycleway().isEscalator());

    var escalator = new OsmWay();
    escalator.addTag("highway", "steps");
    assertFalse(escalator.isEscalator());

    escalator.addTag("conveying", "yes");
    assertTrue(escalator.isEscalator());

    escalator.addTag("conveying", "whoknows?");
    assertFalse(escalator.isEscalator());
  }

  private static OsmWay createGenericHighway() {
    var osm = new OsmWay();
    osm.addTag("highway", "primary");
    return osm;
  }

  private static OsmWay createGenericFootway() {
    var osm = new OsmWay();
    osm.addTag("highway", "footway");
    return osm;
  }

  private static OsmWay createFootway(
    String footwayValue,
    String crossingTag,
    String crossingValue
  ) {
    var osm = createGenericFootway();
    osm.addTag("footway", footwayValue);
    osm.addTag(crossingTag, crossingValue);
    return osm;
  }

  @Test
  void footway() {
    assertFalse(createGenericHighway().isFootway());
    assertTrue(createGenericFootway().isFootway());
  }

  @Test
  void serviceRoad() {
    assertFalse(createGenericHighway().isServiceRoad());

    var osm2 = new OsmWay();
    osm2.addTag("highway", "service");
    assertTrue(osm2.isServiceRoad());
  }

  @ParameterizedTest
  @MethodSource("createCrossingCases")
  void markedCrossing(OsmWay way, boolean result) {
    assertEquals(result, way.isMarkedCrossing());
  }

  static Stream<Arguments> createCrossingCases() {
    return Stream.of(
      Arguments.of(createGenericFootway(), false),
      Arguments.of(createFootway("whatever", "unused", "unused"), false),
      Arguments.of(createFootway("crossing", "crossing", "marked"), true),
      Arguments.of(createFootway("crossing", "crossing", "other"), false),
      Arguments.of(createFootway("crossing", "crossing:markings", "yes"), true),
      Arguments.of(createFootway("crossing", "crossing:markings", "marking-details"), true),
      Arguments.of(createFootway("crossing", "crossing:markings", null), false),
      Arguments.of(createFootway("crossing", "crossing:markings", "no"), false)
    );
  }

  private static OsmWay createPlatform(String kind) {
    var osm = new OsmWay();
    osm.addTag(kind, "platform");
    return osm;
  }

  @ParameterizedTest
  @MethodSource("createTransitPlatformCases")
  void transitPlatform(OsmWay way, boolean result) {
    assertEquals(result, way.isTransitPlatform());
  }

  static Stream<Arguments> createTransitPlatformCases() {
    return Stream.of(
      Arguments.of(createGenericHighway(), false),
      Arguments.of(createGenericFootway(), false),
      Arguments.of(createPlatform("railway"), true),
      Arguments.of(createPlatform("public_transport"), true)
    );
  }

  private OsmWay getClosedPolygon() {
    var way = new OsmWay();
    way.addNodeRef(1);
    way.addNodeRef(2);
    way.addNodeRef(3);
    way.addNodeRef(1);
    return way;
  }
}
