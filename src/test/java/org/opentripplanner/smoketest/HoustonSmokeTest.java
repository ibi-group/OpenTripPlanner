package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Houston can be found.
 */
@Tag("smoke-test")
@Tag("houston")
public class HoustonSmokeTest {

  WgsCoordinate galvestonRoad = new WgsCoordinate(29.6598, -95.2342);
  WgsCoordinate northLindale = new WgsCoordinate(29.8158, -95.3697);

  @Test
  public void routeFromSouthToNorth() {
    SmokeTest.basicTest(
      galvestonRoad,
      northLindale,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "BUS", "BUS", "WALK", "BUS", "WALK")
    );
  }
}
