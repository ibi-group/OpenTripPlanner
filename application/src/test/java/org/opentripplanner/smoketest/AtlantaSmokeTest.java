package org.opentripplanner.smoketest;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.client.parameters.TripPlanParametersBuilder;
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

  private static final Coordinate PEACHTREE_CREEK = new Coordinate(33.7310, -84.3823);
  private static final Coordinate LINDBERH_CENTER = new Coordinate(33.8235, -84.3674);
  private static final Coordinate MADDOX_PARK = new Coordinate(33.7705, -84.4265);
  private static final Coordinate DRUID_HILLS = new Coordinate(33.77933, -84.33689);
  private static final Coordinate FIVE_POINTS = new Coordinate(33.75390, -84.39206);
  private static final Coordinate AIRPORT = new Coordinate(33.64068, -84.44620);

  static List<TripPlanParameters> buildCombinations() {
    return new RequestCombinationsBuilder()
      .withLocations(LINDBERH_CENTER, PEACHTREE_CREEK, MADDOX_PARK, DRUID_HILLS)
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

  @Test
  public void airportToCity() throws IOException {
    var params = new TripPlanParametersBuilder()
      .withFrom(AIRPORT)
      .withTo(FIVE_POINTS)
      .withModes(TRANSIT)
      .withTime(SmokeTest.weekdayAtNoon())
      .build();
    var itins = SmokeTest.API_CLIENT.plan(params).transitItineraries();
    assertThat(itins).isNotEmpty();
    var routes = itins
      .stream()
      .flatMap(i -> i.transitLegs().stream())
      .map(l -> l.route().getLongName())
      .collect(Collectors.toSet());

    assertThat(routes).containsAtLeast("RED", "GOLD");
  }
}
