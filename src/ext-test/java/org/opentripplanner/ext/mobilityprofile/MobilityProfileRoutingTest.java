package org.opentripplanner.ext.mobilityprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobilityProfileRoutingTest {

  @Test
  void canComputeTravelTime() {
    assertEquals(
      0.250 / 1.609 / 2.5,
      MobilityProfileRouting.computeTravelHours(250, MobilityProfile.NONE)
    );
  }
}
