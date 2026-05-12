package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

class TripPatternGeometryTest {

  private static final TransitTestEnvironmentBuilder TEST_ENV = TransitTestEnvironment.of();

  // Three stops roughly 2 km apart along a north-south line at 60° N. Unique id prefix to avoid
  // fixture collisions.
  private static final RegularStop STOP_A = TEST_ENV.stop("TPG-A", b ->
    b.withCoordinate(60.0000, 10.0000)
  );
  private static final RegularStop STOP_B = TEST_ENV.stop("TPG-B", b ->
    b.withCoordinate(60.0180, 10.0000)
  );
  private static final RegularStop STOP_C = TEST_ENV.stop("TPG-C", b ->
    b.withCoordinate(60.0360, 10.0000)
  );

  // A point midway between A and B so the A→B hop geometry is a polyline of three points, not a
  // straight line, letting us exercise the haversine-over-intermediate-coordinates path.
  private static final RegularStop STOP_MID_AB = TEST_ENV.stop("TPG-MID", b ->
    b.withCoordinate(60.0090, 10.0000)
  );

  private static final StopPattern STOP_PATTERN = stopPattern(STOP_A, STOP_B, STOP_C);

  private static final List<LineString> HOP_GEOMETRIES = List.of(
    makeLineString(STOP_A.getCoordinate(), STOP_MID_AB.getCoordinate(), STOP_B.getCoordinate()),
    makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate())
  );

  private static StopPattern stopPattern(StopLocation... stops) {
    var builder = StopPattern.create(stops.length);
    for (int i = 0; i < stops.length; i++) {
      builder.stops.with(i, stops[i]);
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }

  private static double exactDistance(RegularStop from, RegularStop to) {
    return SphericalDistanceLibrary.distance(
      from.getLat(),
      from.getLon(),
      to.getLat(),
      to.getLon()
    );
  }

  @Test
  void distanceBetweenWithHopGeometriesMatchesHaversineWithinOneMeter() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    assertEquals(0, subject.distanceBetween(0, 0));

    // Contract: any distanceBetween(board, alight) is within 1 m of the exact haversine sum.
    double exactAtoB = exactDistance(STOP_A, STOP_B);
    double exactBtoC = exactDistance(STOP_B, STOP_C);
    double exactAtoC = exactAtoB + exactBtoC;

    assertEquals(exactAtoB, subject.distanceBetween(0, 1), 1.0);
    assertEquals(exactBtoC, subject.distanceBetween(1, 2), 1.0);
    assertEquals(exactAtoC, subject.distanceBetween(0, 2), 1.0);
  }

  @Test
  void distanceBetweenFallsBackToStraightLineWhenHopGeometriesAreNull() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, null);

    assertEquals(0, subject.distanceBetween(0, 0));

    double exactAtoB = exactDistance(STOP_A, STOP_B);
    double exactBtoC = exactDistance(STOP_B, STOP_C);
    assertEquals(exactAtoB, subject.distanceBetween(0, 1), 1.0);
    assertEquals(exactBtoC, subject.distanceBetween(1, 2), 1.0);
  }

  @Test
  void cumulativeRoundingErrorIsBoundedByOneMeter() {
    // Build a long pattern (20 stops). Per-hop rounding would accumulate linearly here;
    // verify that accumulating in double and rounding once per entry keeps the error on the
    // full-pattern leg within 1 m.
    int numberOfStops = 20;
    RegularStop[] stops = new RegularStop[numberOfStops];
    for (int i = 0; i < numberOfStops; i++) {
      // ~111 m spacing (at 60°N, 0.001° latitude ≈ 111 m).
      double lat = 60.0 + i * 0.001;
      stops[i] = TEST_ENV.stop("TPG-long-" + i, b -> b.withCoordinate(lat, 10.0));
    }
    StopPattern pattern = stopPattern(stops);

    var subject = TripPatternGeometry.of(pattern, null);

    double exactSum = 0;
    for (int i = 0; i < numberOfStops - 1; i++) {
      exactSum += exactDistance(stops[i], stops[i + 1]);
    }
    assertEquals(exactSum, subject.distanceBetween(0, numberOfStops - 1), 1.0);
  }

  @Test
  void distanceBetweenIsAdditive() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    int partA = subject.distanceBetween(0, 1);
    int partB = subject.distanceBetween(1, 2);
    int whole = subject.distanceBetween(0, 2);

    // Strict equality: differences of cumulative entries cancel exactly.
    assertEquals(whole, partA + partB);
  }

  @Test
  void hopGeometryReturnsCompressedRoundTrip() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    LineString hop0 = subject.hopGeometry(0);
    assertEquals(3, hop0.getNumPoints());
    assertEquals(STOP_A.getLat(), hop0.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_A.getLon(), hop0.getCoordinateN(0).x, 1e-4);
    assertEquals(STOP_B.getLat(), hop0.getCoordinateN(2).y, 1e-4);
    assertEquals(STOP_B.getLon(), hop0.getCoordinateN(2).x, 1e-4);

    LineString hop1 = subject.hopGeometry(1);
    assertEquals(2, hop1.getNumPoints());
    assertEquals(STOP_B.getLat(), hop1.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), hop1.getCoordinateN(1).y, 1e-4);
  }

  @Test
  void hopGeometrySynthesizesStraightLineWhenHopGeometriesAreNull() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, null);

    LineString hop0 = subject.hopGeometry(0);
    assertEquals(2, hop0.getNumPoints());
    assertEquals(STOP_A.getLat(), hop0.getCoordinateN(0).y);
    assertEquals(STOP_B.getLat(), hop0.getCoordinateN(1).y);
  }

  @Test
  void geometryBetweenConcatenatesHopsInRange() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    LineString full = subject.geometryBetween(0, 2);
    // Hop 0: A, MID, B (3 points). Hop 1: B, C (2 points). Duplicate B at the join is dropped.
    assertEquals(4, full.getNumPoints());
    assertEquals(STOP_A.getLat(), full.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), full.getCoordinateN(3).y, 1e-4);

    LineString firstHop = subject.geometryBetween(0, 1);
    assertEquals(3, firstHop.getNumPoints());

    LineString secondHop = subject.geometryBetween(1, 2);
    assertEquals(2, secondHop.getNumPoints());
  }

  @Test
  void concatenatedGeometryReturnsFullPatternWhenShapeAvailable() {
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    LineString full = subject.concatenatedGeometry();
    assertNotNull(full);
    assertEquals(4, full.getNumPoints());
  }

  @Test
  void concatenatedGeometrySynthesizesStraightLineWhenShapeMissing() {
    // After aligning GTFS with NeTEx (#7571), shapeless patterns expose a non-null straight-line
    // geometry made of one 2-point segment per hop, rather than the historical null sentinel.
    var subject = TripPatternGeometry.of(STOP_PATTERN, null);

    LineString full = subject.concatenatedGeometry();
    assertNotNull(full);
    // Two hops of 2 points each, with the duplicated junction at B dropped → 3 points.
    assertEquals(3, full.getNumPoints());
    assertEquals(STOP_A.getLat(), full.getCoordinateN(0).y, 1e-4);
    assertEquals(STOP_C.getLat(), full.getCoordinateN(2).y, 1e-4);
  }

  @Test
  void factoryAcceptsSingleStopPattern() {
    // Degenerate edge case: a pattern with a single stop has no hops. cumulative must still exist
    // and distanceBetween(0,0) must return 0 without throwing. concatenatedGeometry has nothing
    // to concatenate, so it returns an empty (but non-null) LineString.
    StopPattern singleStop = stopPattern(STOP_A);
    var subject = TripPatternGeometry.of(singleStop, null);

    assertEquals(0, subject.distanceBetween(0, 0));
    LineString empty = subject.concatenatedGeometry();
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }

  @Test
  void factoryAcceptsEmptyHopGeometries() {
    // A non-null but empty list must be treated the same as the single-stop degenerate case:
    // factory succeeds, distanceBetween(0,0) is zero, concatenatedGeometry returns an empty
    // (non-null) LineString because there are no hops to concatenate.
    StopPattern singleStop = stopPattern(STOP_A);
    var subject = TripPatternGeometry.of(singleStop, List.of());

    assertEquals(0, subject.distanceBetween(0, 0));
    LineString empty = subject.concatenatedGeometry();
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }

  @Test
  void factoryRejectsHopGeometriesWithWrongSize() {
    // The number of hop geometries must match numberOfStops - 1; a mismatch is a caller bug
    // and must be flagged eagerly rather than silently producing an inconsistent table.
    assertThrows(IllegalArgumentException.class, () ->
      TripPatternGeometry.of(STOP_PATTERN, List.of(HOP_GEOMETRIES.get(0)))
    );
    assertThrows(IllegalArgumentException.class, () ->
      TripPatternGeometry.of(STOP_PATTERN, List.of())
    );
  }

  @Test
  void geometryBetweenWithSameBoardAndAlightReturnsEmptyGeometry() {
    // Board == alight is outside the ScheduledTransitLeg contract but the method is also reachable
    // directly on TripPattern. Lock in the behaviour (empty LineString, no throw).
    var subject = TripPatternGeometry.of(STOP_PATTERN, HOP_GEOMETRIES);

    LineString empty = subject.geometryBetween(1, 1);
    assertNotNull(empty);
    assertTrue(empty.isEmpty());
  }
}
