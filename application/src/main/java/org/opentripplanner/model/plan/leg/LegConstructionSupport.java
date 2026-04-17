package org.opentripplanner.model.plan.leg;

import java.util.ArrayList;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Utility methods for constructing legs.
 */
public class LegConstructionSupport {

  /**
   * Given a pattern, board and alight stop index compute the list of coordinates that this
   * segment of the pattern visits.
   */
  public static LineString extractSubGeometry(
    TripPattern tripPattern,
    int boardStopIndexInPattern,
    int alightStopIndexInPattern
  ) {
    var lineStrings = new ArrayList<LineString>();
    for (int i = boardStopIndexInPattern + 1; i <= alightStopIndexInPattern; i++) {
      lineStrings.add(tripPattern.getHopGeometry(i - 1));
    }
    return GeometryUtils.concatenateLineStrings(lineStrings);
  }
}
