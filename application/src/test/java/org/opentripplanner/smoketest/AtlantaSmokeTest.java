package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.smoketest.util.RequestCombinationsBuilder;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from central Atlanta to the flex zone in Powder
 * Springs can be planned. In order to guarantee somewhat predictable results over time it uses 1pm
 * on the next Monday relative to the day the test is run as the start time of the route.
 * <p>
 * The assertions are intentionally vague as more precise ones would probably cause false positives
 * when there are slight changes in the schedule.
 */
@Tag("smoke-test")
@Tag("atlanta")
public class AtlantaSmokeTest {

  static Coordinate peachtreeCreek = new Coordinate(33.7310, -84.3823);
  static Coordinate lindberghCenter = new Coordinate(33.8235, -84.3674);
  static Coordinate maddoxPark = new Coordinate(33.7705, -84.4265);
  static Coordinate druidHills = new Coordinate(33.77933, -84.33689);

  static List<TripPlanParameters> buildCombinations() {
    return new RequestCombinationsBuilder()
      .withLocations(lindberghCenter, peachtreeCreek, maddoxPark, druidHills)
      .withModes(TRANSIT, WALK)
      .withTime(SmokeTest.weekdayAtNoon())
      .includeWheelchair()
      .includeArriveBy()
      .build();
  }

  @ParameterizedTest
  @MethodSource("buildCombinations")
  public void accessibleRouting(TripPlanParameters params) throws IOException {
    var tripPlan = SmokeTest.API_CLIENT.plan(params);
    assertFalse(tripPlan.transitItineraries().isEmpty());
  }
}
