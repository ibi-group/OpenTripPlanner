package org.opentripplanner.routing.api.request.request.filter;

import org.opentripplanner.transit.model.timetable.TransitFilterable;
import org.opentripplanner.transit.model.timetable.TripTimes;

public interface TransitFilter {
  /**
   * Return false is trip pattern is banned, otherwise return true
   */
  boolean matchFilterable(TransitFilterable tripPattern);

  /**
   * Return false is tripTimes are banned, otherwise return true
   */
  boolean matchTripTimes(TripTimes trip);

  default boolean isSubModePredicate() {
    return false;
  }

  default boolean isModePredicate() {return false;}
}
