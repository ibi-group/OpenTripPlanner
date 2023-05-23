package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

/**
 * This filter will remove all itineraries that are outside the search-window. In some
 * cases the access is time-shifted after the end of the search-window. These results
 * should appear again when paging to the next page. Hence, this filter will remove
 * such itineraries.
 */
public class OutsideSearchWindowFilter implements ItineraryDeletionFlagger {

  public static final String TAG = "outside-search-window";

  private final Instant limit;

  public OutsideSearchWindowFilter(Instant latestDepartureTime) {
    this.limit = latestDepartureTime;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> it.startTime().toInstant().isAfter(limit);
  }

  @Override
  public boolean skipAlreadyFlaggedItineraries() {
    return false;
  }

  /**
   * Return {@code true} if given {@code itinerary} is tagged by this filter.
   */
  public static boolean taggedBy(Itinerary itinerary) {
    return itinerary.hasSystemNoticeTag(TAG);
  }
}
