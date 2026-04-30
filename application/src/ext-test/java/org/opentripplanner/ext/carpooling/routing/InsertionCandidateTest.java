package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder.createGraphPath;
import static org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder.createGraphPaths;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;

class InsertionCandidateTest {

  private static final Duration STOP_DURATION = Duration.ofMinutes(2);

  @Test
  void totalTripDuration_calculatesFromSegments() {
    // Simple trip origin → destination, with passenger pickup and dropoff inserted:
    // origin → pickup (5 min) → dropoff (10 min) → destination (8 min)
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var originToPickup = createGraphPath(Duration.ofMinutes(5));
    var pickupToDropoff = createGraphPath(Duration.ofMinutes(10));
    var dropoffToDestination = createGraphPath(Duration.ofMinutes(8));

    var candidate = new InsertionCandidate(
      trip,
      1,
      2,
      List.of(originToPickup, pickupToDropoff, dropoffToDestination),
      STOP_DURATION,
      null
    );

    // 5 + 2 (stop) + 10 + 2 (stop) + 8 = 27 minutes
    assertEquals(Duration.ofMinutes(27), candidate.totalTripDuration());
  }

  @Test
  void getPickupSegments_returnsCorrectRange() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(5);

    var candidate = new InsertionCandidate(trip, 2, 4, segments, STOP_DURATION, null);

    var pickupSegments = candidate.getPickupSegments();
    assertEquals(2, pickupSegments.size());
    assertEquals(segments.subList(0, 2), pickupSegments);
  }

  @Test
  void getPickupSegments_positionZero_returnsEmpty() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(3);

    var candidate = new InsertionCandidate(trip, 0, 2, segments, STOP_DURATION, null);

    var pickupSegments = candidate.getPickupSegments();
    assertTrue(pickupSegments.isEmpty());
  }

  @Test
  void getSharedSegments_returnsCorrectRange() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(5);

    var candidate = new InsertionCandidate(trip, 1, 3, segments, STOP_DURATION, null);

    var sharedSegments = candidate.getSharedSegments();
    assertEquals(2, sharedSegments.size());
    assertEquals(segments.subList(1, 3), sharedSegments);
  }

  @Test
  void getSharedSegments_adjacentPositions_returnsSingleSegment() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(3);

    var candidate = new InsertionCandidate(trip, 1, 2, segments, STOP_DURATION, null);

    var sharedSegments = candidate.getSharedSegments();
    assertEquals(1, sharedSegments.size());
  }

  @Test
  void getDropoffSegments_returnsCorrectRange() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(5);

    var candidate = new InsertionCandidate(trip, 1, 3, segments, STOP_DURATION, null);

    var dropoffSegments = candidate.getDropoffSegments();
    assertEquals(2, dropoffSegments.size());
    assertEquals(segments.subList(3, 5), dropoffSegments);
  }

  @Test
  void getDropoffSegments_atEnd_returnsEmpty() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(3);

    var candidate = new InsertionCandidate(trip, 1, 3, segments, STOP_DURATION, null);

    var dropoffSegments = candidate.getDropoffSegments();
    assertTrue(dropoffSegments.isEmpty());
  }

  @Test
  void toString_includesKeyInformation() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var segments = createGraphPaths(3);

    var candidate = new InsertionCandidate(trip, 1, 2, segments, STOP_DURATION, null);

    var str = candidate.toString();
    assertTrue(str.contains("pickup@1"));
    assertTrue(str.contains("dropoff@2"));
    assertTrue(str.contains("duration="));
    assertTrue(str.contains("segments=3"));
  }

  /**
   * No pickup segments → durationUntilPickup is zero and no boarding dwell is added to the ride.
   * Single shared segment → passengerRideDuration is just the segment duration.
   */
  @Test
  void durations_noPickupSegments_singleSharedSegment() {
    var stopDuration = Duration.ofMinutes(2);
    var sharedPath = createGraphPath(Duration.ofMinutes(10));
    var sharedDuration = GraphPathUtils.calculateDuration(sharedPath);

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var candidate = new InsertionCandidate(trip, 0, 1, List.of(sharedPath), stopDuration, null);

    assertEquals(Duration.ofMinutes(10), sharedDuration);
    assertEquals(Duration.ZERO, candidate.getDurationUntilPickupArrival());
    assertEquals(sharedDuration, candidate.getPassengerRideDuration());
  }

  /**
   * Single pickup segment → durationUntilPickup = segment duration (boarding excluded).
   * Single shared segment → passengerRideDuration = boarding time + segment duration.
   */
  @Test
  void durations_onePickupSegment_singleSharedSegment() {
    var stopDuration = Duration.ofMinutes(3);
    var pickupPath = createGraphPath(Duration.ofMinutes(8));
    var sharedPath = createGraphPath(Duration.ofMinutes(15));

    var pickupDuration = GraphPathUtils.calculateDuration(pickupPath);
    var sharedDuration = GraphPathUtils.calculateDuration(sharedPath);

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var candidate = new InsertionCandidate(
      trip,
      1,
      2,
      List.of(pickupPath, sharedPath),
      stopDuration,
      null
    );

    assertEquals(pickupDuration, candidate.getDurationUntilPickupArrival());
    assertEquals(stopDuration.plus(sharedDuration), candidate.getPassengerRideDuration());
  }

  /**
   * Two pickup segments → travel + 1 intermediate stop, no boarding dwell.
   * Two shared segments → boarding dwell + travel + 1 intermediate stop.
   */
  @Test
  void durations_multiplePickupAndSharedSegments() {
    var stopDuration = Duration.ofMinutes(2);
    var pickup0 = createGraphPath(Duration.ofMinutes(5));
    var pickup1 = createGraphPath(Duration.ofMinutes(7));
    var shared0 = createGraphPath(Duration.ofMinutes(10));
    var shared1 = createGraphPath(Duration.ofMinutes(12));

    var pickup0Duration = GraphPathUtils.calculateDuration(pickup0);
    var pickup1Duration = GraphPathUtils.calculateDuration(pickup1);
    var shared0Duration = GraphPathUtils.calculateDuration(shared0);
    var shared1Duration = GraphPathUtils.calculateDuration(shared1);

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var candidate = new InsertionCandidate(
      trip,
      2,
      4,
      List.of(pickup0, pickup1, shared0, shared1),
      stopDuration,
      null
    );

    // 2 pickup segments: travel + 1 intermediate stop (boarding now belongs to the ride)
    var expectedPickup = pickup0Duration.plus(stopDuration).plus(pickup1Duration);
    assertEquals(expectedPickup, candidate.getDurationUntilPickupArrival());

    // 2 shared segments: boarding dwell + travel + 1 intermediate stop delay
    var expectedRide = stopDuration.plus(shared0Duration).plus(stopDuration).plus(shared1Duration);
    assertEquals(expectedRide, candidate.getPassengerRideDuration());
  }

  /**
   * Larger stop duration scales the durations proportionally.
   */
  @Test
  void durations_scaleWithStopDuration() {
    var shared0 = createGraphPath(Duration.ofMinutes(10));
    var shared1 = createGraphPath(Duration.ofMinutes(10));

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var candidateSmall = new InsertionCandidate(
      trip,
      0,
      2,
      List.of(shared0, shared1),
      Duration.ofMinutes(1),
      null
    );
    var candidateLarge = new InsertionCandidate(
      trip,
      0,
      2,
      List.of(shared0, shared1),
      Duration.ofMinutes(5),
      null
    );

    // Pickup at origin (no pickup segments) → no boarding dwell, so only the 1 intermediate
    // stop between the 2 shared segments scales: 1x stopDuration difference.
    var difference = candidateLarge
      .getPassengerRideDuration()
      .minus(candidateSmall.getPassengerRideDuration());
    assertEquals(Duration.ofMinutes(4), difference);
  }
}
