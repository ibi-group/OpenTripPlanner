package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.passThrough;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.via.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to handle a route request with a specified pass-through point.
 * If a stop point is specified as a pass-through point in the request, then all the results
 * returned from Raptor should include this stop point either as an alight or board point for a
 * trip or as an intermediate point in the trip.
 *
 * It should be possible to specify more than one pass-through point. The result should include
 * stop points in the order in which they were specified in the request. Only alternatives that
 * pass through all stop points should be included in the result.
 *
 * In order to support stop areas raptor should also support multiple stop points in the same
 * pass-through group. It should be possible to define both stop A and B as a pass-through. Then
 * alternatives that pass either stop A or B should not be dropped.
 */
class J01_PassThroughTest {

  static final RaptorViaLocation PASS_THROUGH_STOP_A = passThrough("A").addStop(STOP_A).build();
  static final RaptorViaLocation PASS_THROUGH_STOP_C = passThrough("C").addStop(STOP_C).build();
  static final RaptorViaLocation PASS_THROUGH_STOP_D = passThrough("D").addStop(STOP_D).build();
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_OR_C = List.of(
    passThrough("B|C").addStop(STOP_B, STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_C = List.of(
    passThrough("B").addStop(STOP_B).build(),
    passThrough("C").addStop(STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_D = List.of(
    passThrough("B").addStop(STOP_B).build(),
    passThrough("D").addStop(STOP_D).build()
  );

  private final TestTransitData data = new TestTransitData();

  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  private RaptorRequestBuilder<TestTripSchedule> prepareRequest() {
    var builder = data.requestBuilder();

    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with pass-through so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed
      .clearOptimizations();

    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(8))
      .timetable(true);

    return builder;
  }

  @Test
  @DisplayName("Pass-through stop point as a last point in the journey.")
  void passThroughPointOnEgress() {
    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the last stop in the trip.
    // The trip with pass-through point has significantly longer travel time so that normally it
    //  should not be used
    data.withTimetables(
      """
      -- R1
      A     B     C
      0:02  0:05  0:20
      -- R2
      A     B           D
      0:02  0:10        0:50
      """
    );

    data.access("Walk 30s ~ A").egress("D ~ Walk 30s", "C ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_D);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as a first point in the journey.")
  void passThroughPointOnAccess() {
    data
      .access("Walk 30s ~ A", "Walk 30s ~ B")
      // Create two routes.
      // Only one of them includes required pass-through point.
      // Pass-through point is the first stop in the trip.
      // The trip with pass-through point has significantly longer travel time so that normally it
      //  should not be used
      .withTimetables(
        """
              B     C     D
              0:02  0:05  0:20
        ---
        A           C     D
        0:02        0:10  0:50
        """
      )
      .egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_A);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as an intermediate point in the journey.")
  void passThroughPointInTheMiddle() {
    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the intermediate stop in the trip.
    // The trip with pass-through point has significantly longer travel time so that normally it
    //  should not be used
    data.withTimetables(
      """
      -- R1
      A     B     D
      0:02  0:05  0:20
      -- R2
      A     C     D
      0:02  0:10  0:50
      """
    );
    data.withTransferCost(100);
    data.access("Walk 30s ~ A").egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_C);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple pass-through stop points")
  void multiplePassThroughPoints() {
    // Create two routes.
    // First one includes one pass-through stop point.
    // The second one includes the second pass-through point.
    // Both arrive at the desired destination so normally there should not be any transfers.
    data.withTimetables(
      """
      -- R1
      A     B     C                 F
      0:02  0:05  0:10              0:20
      -- R2
                  C     D     E     F
                  0:15  0:20  0:30  0:50
      """
    );

    data.withTransferCost(100);
    data.access("Walk 30s ~ A").egress("F ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_THEN_D);

    // Verify that Raptor generated journey with a transfer to r2 so that both pass-through points
    //  are included
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ C ~ BUS R2 0:15 0:50 ~ F ~ Walk 30s [0:01:30 0:50:30 49m Tₙ1 C₁4_300]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through order")
  void passThroughOrder() {
    // Create two routes.
    // Both include all the desired pass-through stop points but only one of them have correct order.
    data.withTimetables(
      """
      -- R1
      A     B     C     D
      0:05  0:10  0:15  0:20
      -- R2
      A     C     B     D
      0:05  0:10  0:15  0:17
      """
    );

    data.access("Walk 30s ~ A").egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_THEN_C);

    // Verify that only route with correct pass-through order is returned
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:05 0:20 ~ D ~ Walk 30s [0:04:30 0:20:30 16m Tₙ0 C₁1_620]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple stops in same pass-through group")
  void passThroughGroup() {
    // Create two routes.
    // Route one includes STOP_B and route two includes STOP_C.
    // Both stops will be part of the same pass-through group
    //  so that both routes should be valid
    data.withTimetables(
      """
      A     C     E
      0:04  0:10  0:15
      --
      B     D     E
      0:05  0:10  0:14
      """
    );

    // Both routes are pareto optimal.
    // Route 2 is faster but it contains more walk
    data.access("Walk 30s ~ A", "Walk 2m ~ B").egress("E ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_OR_C);

    // Verify that both routes are included as a valid result
    assertEquals(
      """
      Walk 2m ~ B ~ BUS R2 0:05 0:14 ~ E ~ Walk 30s [0:03 0:14:30 11m30s Tₙ0 C₁1_440]
      Walk 30s ~ A ~ BUS R1 0:04 0:15 ~ E ~ Walk 30s [0:03:30 0:15:30 12m Tₙ0 C₁1_380]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName(
    "The pass-through is implemented with listeners at the alight stop. If another path exists at " +
      "the same stop, then this should have no effect on the pass-through connection."
  )
  void passThroughStopVisitShouldNotBeDominatedByAnotherPath() {
    // Create two routes.
    // Route one includes STOP_B and route two includes STOP_C.
    // Both stops will be part of the same pass-through group
    //  so that both routes should be valid
    data.withTimetables(
      """
      -- R1
      A     C
      0:01  0:10
      -- R2
      A     C     E
      0:00  0:10  0:15
      """
    );

    // Both routes are pareto optimal.
    // Route 2 is faster but it contains more walk
    data.access("Free ~ A").egress("E ~ Free");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_C);

    // R2 is the only path which takes you to the destination, so the state should be copied over
    // from segment 1 to segment 2 after stop C is "passed-through". Even when R1 ≺ R2 at stop
    // arrival at stop C. R1 is optimal compared to R2 at stop arrival C, because the cost and
    // departure time is better, while number of transfers and arrival time is the same.
    assertEquals(
      """
      A ~ BUS R2 0:00 0:15 ~ E [0:00 0:15 15m Tₙ0 C₁1_500]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
