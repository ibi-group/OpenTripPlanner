package org.opentripplanner.smoketest;

import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;
import static org.opentripplanner.smoketest.SeattleSmokeTest.LYNNWOOD_STA;
import static org.opentripplanner.smoketest.SeattleSmokeTest.OLIVE_WAY;
import static org.opentripplanner.smoketest.SeattleSmokeTest.SHORELINE;
import static org.opentripplanner.smoketest.SeattleSmokeTest.SODO;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.smoketest.util.SmokeTestItinerary;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("soundtransit")
public class SoundTransitSmokeTest {

  @Test
  public void testItinerary() {
    var modes = Set.of(TRANSIT, WALK);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(OLIVE_WAY, SHORELINE, modes),
      List.of("WALK", "BUS", "WALK")
    );

    SmokeTestItinerary
      .from(plan)
      .hasLeg()
      .withMode("BUS")
      .withRouteShortName("E Line")
      .withFarePrice(2.75f)
      .assertMatches();

    plan =
      SmokeTest.basicRouteTest(
        new SmokeTestRequest(LYNNWOOD_STA, SODO, modes),
        List.of("WALK", "TRAM", "WALK")
      );

    SmokeTestItinerary
      .from(plan)
      .hasLeg()
      .withMode("TRAM")
      .withRouteShortName("1 Line")
      .withFarePrice(3f)
      .assertMatches();
  }
}
