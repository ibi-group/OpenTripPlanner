package org.opentripplanner.smoketest.util;

import java.util.Set;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.parameters.TripPlanParameters.SearchDirection;

public record SmokeTestRequest(
  Coordinate from,
  Coordinate to,
  Set<RequestMode> modes,
  boolean arriveBy
) {
  public SmokeTestRequest(Coordinate from, Coordinate to, Set<RequestMode> modes) {
    this(from, to, modes, false);
  }

  public SearchDirection searchDirection() {
    if (arriveBy) {
      return SearchDirection.ARRIVE_BY;
    } else {
      return SearchDirection.DEPART_AT;
    }
  }
}
