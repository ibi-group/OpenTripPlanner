package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.VehicleModesEnumeration;

class ExtraJourneyTest implements RealtimeTestConstants {

  private static final String ADDED_TRIP_ID = "newJourney";
  private static final String OPERATOR_ID = "operatorId";
  private static final String ROUTE_ID = "routeId";
  private static final String RAIL_ROUTE_ID = "railRouteId";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final Operator OPERATOR = ENV_BUILDER.operator(OPERATOR_ID);
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID, OPERATOR);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  private final Route RAIL_ROUTE = ENV_BUILDER.route(RAIL_ROUTE_ID, r ->
    r.withMode(TransitMode.RAIL)
  );

  private final TripInput RAIL_TRIP_INPUT = TripInput.of("railTrip1")
    .withRoute(RAIL_ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testAddJourneyWithExistingRoute() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    Route route = ROUTE;
    int numPatternForRoute = env.transitService().findPatterns(route).size();

    var updates = createValidAddedJourney(siri).buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "ADDED | C [R] 0:02 0:02 | D 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | C 0:01 0:01 | D 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );
    FeedScopedId tripId = id(ADDED_TRIP_ID);
    TransitService transitService = env.transitService();
    Trip trip = transitService.getTrip(tripId);
    assertNotNull(trip);
    assertNotNull(transitService.findPattern(trip));
    assertNotNull(transitService.getTripOnServiceDate(tripId));
    assertNotNull(
      transitService.getTripOnServiceDate(
        new TripIdAndServiceDate(tripId, env.defaultServiceDate())
      )
    );
    assertEquals(
      numPatternForRoute + 1,
      transitService.findPatterns(route).size(),
      "The added trip should use a new pattern for this route"
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:routeId::001:RT[ADDED]"
    );
  }

  @Test
  void testAddJourneyWithNewRoute() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "new route ref";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    int numRoutes = env.transitService().listRoutes().size();
    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "ADDED | C [R] 0:02 0:02 | D 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | C 0:01 0:01 | D 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );
    TransitService transitService = env.transitService();
    assertEquals(numRoutes + 1, transitService.listRoutes().size());
    FeedScopedId newRouteId = id(newRouteRef);
    Route newRoute = transitService.getRoute(newRouteId);
    assertNotNull(newRoute);
    assertEquals(1, transitService.findPatterns(newRoute).size());

    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:new route ref::001:RT[ADDED]"
    );
  }

  @Test
  void testAddJourneyMultipleTimes() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = createValidAddedJourney(siri).buildEstimatedTimetableDeliveries();

    int numTrips = env.transitService().listTrips().size();
    assertSuccess(siri.applyEstimatedTimetable(updates));
    assertEquals(numTrips + 1, env.transitService().listTrips().size());
    assertSuccess(siri.applyEstimatedTimetable(updates));
    assertEquals(numTrips + 1, env.transitService().listTrips().size());

    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:routeId::001:RT[UPDATED]"
    );
  }

  @Test
  void testAddedJourneyWithInvalidScheduledData() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Create an extra journey with invalid planned data (travel back in time)
    // and valid real time data
    var createExtraJourney = siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("10:58", "10:48")
          .call(STOP_B)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testReplaceJourney() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_C).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertEquals(
      "ADDED | A [R] 0:02 0:02 | C 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 0:01 0:01 | C 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );

    // Original trip should not get canceled
    var originalTripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:routeId::001:RT[ADDED]"
    );
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:02")
          .call(STOP_C)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(UpdateError.UpdateErrorType.UNKNOWN, result);
  }

  @Test
  void testAddJourneyWithNewRouteAndShortName() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "newRouteForShortName";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .withPublishedLineName("L1")
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    Route newRoute = env.transitService().getRoute(id(newRouteRef));
    assertEquals(
      "L1",
      newRoute.getShortName(),
      "PublishedLineName should be mapped to the new route short name"
    );
  }

  @Test
  void testAddJourneyWithNewRouteAndOperator() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "newRouteForOperator";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    Trip trip = env.transitService().getTrip(id(ADDED_TRIP_ID));
    assertNotNull(trip);
    assertEquals(
      OPERATOR,
      trip.getOperator(),
      "The new trip is assigned to the operator specified in OperatorRef"
    );
  }

  @Test
  void testAddJourneyWithNewRouteResolvesAgencyFromOperator() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "newRouteForAgency";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());

    Route newRoute = env.transitService().getRoute(id(newRouteRef));
    assertNotNull(newRoute);
    assertEquals(
      ROUTE.getAgency(),
      newRoute.getAgency(),
      "The new route agency should be mapped to the agency of any existing route that has the same operator"
    );
  }

  @Test
  void testAddJourneyBusReplacingRailHasRailReplacementSubmode() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(RAIL_TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "busReplacementRoute";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .withExternalLineRef(RAIL_ROUTE_ID)
      .withVehicleMode(VehicleModesEnumeration.BUS)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    Route newRoute = env.transitService().getRoute(id(newRouteRef));
    assertNotNull(newRoute);
    assertEquals(TransitMode.BUS, newRoute.getMode());
    assertEquals(
      SubMode.of("railReplacementBus"),
      newRoute.getNetexSubmode(),
      "When an added bus trip is assigned to an existing rail route, the submode should be 'railReplacementBus'"
    );
  }

  @Test
  void testAddJourneyRailReplacingRailHasReplacementRailSubmode() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(RAIL_TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "railReplacementRoute";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .withExternalLineRef(RAIL_ROUTE_ID)
      .withVehicleMode(VehicleModesEnumeration.RAIL)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    Route newRoute = env.transitService().getRoute(id(newRouteRef));
    assertNotNull(newRoute);
    assertEquals(TransitMode.RAIL, newRoute.getMode());
    assertEquals(
      SubMode.of("replacementRailService"),
      newRoute.getNetexSubmode(),
      "When an added rail trip is assigned to an existing rail route, the submode should be 'replacementRailService'"
    );
  }

  private SiriEtBuilder createValidAddedJourney(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_C).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_D).arriveAimedExpected("00:03", "00:04"));
  }
}
