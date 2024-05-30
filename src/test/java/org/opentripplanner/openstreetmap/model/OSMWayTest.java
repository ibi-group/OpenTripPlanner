package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;

class OSMWayTest {

  @Test
  void testIsBicycleDismountForced() {
    OSMWay way = new OSMWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  void testIsSteps() {
    OSMWay way = new OSMWay();
    assertFalse(way.isSteps());

    way.addTag("highway", "primary");
    assertFalse(way.isSteps());

    way.addTag("highway", "steps");
    assertTrue(way.isSteps());
  }

  @Test
  void wheelchairAccessibleStairs() {
    var osm1 = new OSMWay();
    osm1.addTag("highway", "steps");
    assertFalse(osm1.isWheelchairAccessible());

    // explicitly suitable for wheelchair users, perhaps because of a ramp
    var osm2 = new OSMWay();
    osm2.addTag("highway", "steps");
    osm2.addTag("wheelchair", "yes");
    assertTrue(osm2.isWheelchairAccessible());
  }

  @Test
  void testIsRoundabout() {
    OSMWay way = new OSMWay();
    assertFalse(way.isRoundabout());

    way.addTag("junction", "dovetail");
    assertFalse(way.isRoundabout());

    way.addTag("junction", "roundabout");
    assertTrue(way.isRoundabout());
  }

  @Test
  void testIsOneWayDriving() {
    OSMWay way = new OSMWay();
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
    OSMWay way = new OSMWay();
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
    OSMWay way = new OSMWay();
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

    var escalator = new OSMWay();
    escalator.addTag("highway", "steps");
    assertFalse(escalator.isEscalator());

    escalator.addTag("conveying", "yes");
    assertTrue(escalator.isEscalator());

    escalator.addTag("conveying", "whoknows?");
    assertFalse(escalator.isEscalator());
  }

  private static OSMWay createGenericHighway() {
    var osm = new OSMWay();
    osm.addTag("highway", "primary");
    return osm;
  }

  private static OSMWay createGenericFootway() {
    var osm = new OSMWay();
    osm.addTag("highway", "footway");
    return osm;
  }

  private static OSMWay createFootway(
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

    var osm2 = new OSMWay();
    osm2.addTag("highway", "service");
    assertTrue(osm2.isServiceRoad());
  }

  @ParameterizedTest
  @MethodSource("createCrossingCases")
  void markedCrossing(OSMWay way, boolean result) {
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
}
