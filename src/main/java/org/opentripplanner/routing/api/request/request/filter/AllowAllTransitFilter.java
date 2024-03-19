package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.timetable.TransitFilterable;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * This filter will include everything.
 */
public class AllowAllTransitFilter implements Serializable, TransitFilter {

  private static final AllowAllTransitFilter INSTANCE = new AllowAllTransitFilter();

  private AllowAllTransitFilter() {}

  public static AllowAllTransitFilter of() {
    return INSTANCE;
  }

  @Override
  public boolean matchFilterable(TransitFilterable ignored) {
    return true;
  }

  @Override
  public boolean matchTripTimes(TripTimes trip) {
    return true;
  }
}
