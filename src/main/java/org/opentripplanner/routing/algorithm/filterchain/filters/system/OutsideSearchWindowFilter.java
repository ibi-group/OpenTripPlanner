package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter will remove all depart-at itineraries that are outside the search-window. In some
 * cases the access is time-shifted after the end of the search-window. These results
 * should appear again when paging to the next page. Hence, this filter will remove
 * such itineraries. The same is true for when paging to the previous page for arriveBy=true.
 * <p>
 * Itineraries matching the start(earliest-departure-time) are included and itineraries matching
 * the end(latest-departure-time) are not. The filter is {@code [inclusive, exclusive]}.
 * <p>
 * Arrive-by searches results are treated differently:
 * <p>
 * Transit result are also filtered by their departure time and whether they don't depart
 * after the end of the computed search window which is dependent on the heuristic's minimum
 * transit time. This is identical to the depart-at searches.
 * </p>
 * However, it doesn't work for street/flex-only because they can be shorter than the transit ones and often
 * end up time-shifted right up to the arrive-by time.
 * <p>
 * For that reason the arrive-by street/flex-only results are only checked if they start after the
 * latest-departure-time.
 * <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/6046">Further reading.</a>
 */
public class OutsideSearchWindowFilter implements RemoveItineraryFlagger {

  public static final String TAG = "outside-search-window";

  private final Instant earliestDepartureTime;
  private final Instant latestDepartureTime;
  private final boolean isArriveBy;

  public OutsideSearchWindowFilter(
    Instant earliestDepartureTime,
    Duration searchWindow,
    boolean isArriveBy
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.latestDepartureTime = earliestDepartureTime.plus(searchWindow);
    this.isArriveBy = isArriveBy;
  }

  @Override
  public String name() {
    return TAG;
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return it -> {
      var startTime = it.startTime().toInstant();
      if (it.isOnStreetAndFlexOnly() && isArriveBy) {
        return startTime.isBefore(earliestDepartureTime);
      } else {
        return (
          startTime.isBefore(earliestDepartureTime) || !startTime.isBefore(latestDepartureTime)
        );
      }
    };
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
