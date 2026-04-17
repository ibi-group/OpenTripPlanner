package org.opentripplanner.raptor.rangeraptor.internalapi;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Holds all on-board trip access arrivals for a single route, indexed by stop position in the
 * route's stop pattern.
 *
 * <p>An on-board trip access represents a passenger who is already on board a vehicle at the
 * start of the search — i.e., their "access leg" is the ride itself rather than a walk to a
 * stop. Each such arrival carries a {@code RaptorTripScheduleStopPosition}
 * that identifies exactly which route, trip, and stop position the passenger must board from.
 */
public final class OnBoardTripAccessPathsForRoute<T extends RaptorTripSchedule> {

  private final TIntObjectMap<List<ArrivalView<T>>> arrivals = new TIntObjectHashMap<>();

  /** Returns {@code true} if there are on-board arrivals waiting to board at {@code stopPos}. */
  public boolean containsKey(int stopPos) {
    return arrivals.containsKey(stopPos);
  }

  /** Returns all on-board arrivals that should board at {@code stopPos}. */
  public Iterable<? extends ArrivalView<T>> listArrivals(int stopPos) {
    return arrivals.get(stopPos);
  }

  /** Adds an on-board arrival, indexing it by its boarding stop position in the pattern. */
  public void add(ArrivalView<T> arrival) {
    var boardingConstraint = arrival.subsequentBoardingConstraint();
    var list = arrivals.get(boardingConstraint.stopPositionInPattern());
    if (list == null) {
      list = new ArrayList<>();
      arrivals.put(boardingConstraint.stopPositionInPattern(), list);
    }
    list.add(arrival);
  }
}
