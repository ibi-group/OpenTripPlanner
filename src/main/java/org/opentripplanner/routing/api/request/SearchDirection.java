package org.opentripplanner.routing.api.request;

/**
 * Qualifier for the time by the routing request specified, which can either mean to depart at
 * or arrive by a certain time.
 */
public enum SearchDirection {
  DEPART_AT,
  ARRIVE_BY;

  public boolean isArriveBy() {
    return this == ARRIVE_BY;
  }
}
