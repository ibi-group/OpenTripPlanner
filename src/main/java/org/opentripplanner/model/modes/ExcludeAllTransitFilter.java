package org.opentripplanner.model.modes;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * This filter will exclude everything.
 */
public final class ExcludeAllTransitFilter implements Serializable, TransitFilter {

  private static final ExcludeAllTransitFilter INSTANCE = new ExcludeAllTransitFilter();

  private ExcludeAllTransitFilter() {}

  public static ExcludeAllTransitFilter of() {
    return INSTANCE;
  }

  @Override
  public boolean matchRoute(Route route) {
    return false;
  }

  @Override
  public boolean matchTripTimes(TripTimes trip) {
    return false;
  }
}
