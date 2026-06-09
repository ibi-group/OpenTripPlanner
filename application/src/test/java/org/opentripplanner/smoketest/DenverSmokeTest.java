package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.smoketest.util.RequestCombinationsBuilder;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Denver can be found.
 */
@Tag("smoke-test")
@Tag("denver")
public class DenverSmokeTest {

  private static final Coordinate SOUTH_BROADWAY = new Coordinate(39.7020, -104.9866);
  private static final Coordinate TWIN_LAKE = new Coordinate(39.8232, -105.0055);
  private static final Coordinate LAKE_WOOD = new Coordinate(39.71471, -105.08612);
  private static final Coordinate UNION_STATION = new Coordinate(39.75282, -104.99986);
  private static final Coordinate NATIONAL_WESTERN_CENTER = new Coordinate(39.78384, -104.96716);

  @Test
  public void routeFromSouthToNorth() {
    var modes = Set.of(TRANSIT, WALK);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(SOUTH_BROADWAY, TWIN_LAKE, modes),
      List.of("WALK", "TRAM", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }

  static List<TripPlanParameters> buildCombinations() {
    return new RequestCombinationsBuilder()
      .withLocations(SOUTH_BROADWAY, TWIN_LAKE, LAKE_WOOD, UNION_STATION, NATIONAL_WESTERN_CENTER)
      .withModes(TRANSIT, WALK)
      .withTime(SmokeTest.weekdayAtNoon())
      .includeArriveBy()
      .build();
  }

  @ParameterizedTest
  @MethodSource("buildCombinations")
  public void combinations(TripPlanParameters params) throws IOException {
    var tripPlan = SmokeTest.API_CLIENT.plan(params);
    assertFalse(tripPlan.transitItineraries().isEmpty());
  }
}
