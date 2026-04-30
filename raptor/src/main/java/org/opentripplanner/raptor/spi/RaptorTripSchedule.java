package org.opentripplanner.raptor.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * The purpose of this interface is to provide information about the trip schedule. The trip is a
 * child of, and lives in the context of a trip pattern.
 * <p/>
 * The purpose of hiding these attributes behind an interface is to allow the implementation to
 * choose the most efficient underlying representation that suits its needs.
 */
public interface RaptorTripSchedule {
  /**
   * An id/index for the trip which can be used to sort trips so they follow each other in time. The
   * id/index must increase with the departure time.
   */
  int tripSortIndex();

  /**
   * The arrival time at the given stop position in pattern.
   *
   * @param stopPosInPattern the stop position.
   * @return the arrival time in seconds at the given stop
   */
  int arrival(int stopPosInPattern);

  /**
   * Search for the arrival time for the given stopIndex. This is not optimized for performance.
   *
   * @param startStopPos the stop position in pattern to start the search (inclusive).
   * @param stopIndex    the stop index to find the arrival time for.
   * @return the arrival time in seconds at the given stop
   * @throws IndexOutOfBoundsException if {@code stopIndex} is not found
   */
  default int arrival(int startStopPos, int stopIndex) {
    return arrival(pattern().findStopPositionAfter(startStopPos, stopIndex));
  }

  /**
   * The departure time at the given stop position in pattern.
   *
   * @param stopPosInPattern the stop position.
   * @return the departure time in seconds at the given stop
   */
  int departure(int stopPosInPattern);

  /**
   * Search for the departure time for the given stopIndex. This is not optimized for performance.
   *
   * @param startStopPos the stop position in pattern to start the search (inclusive).
   * @param stopIndex    the stop index to find the departure time for.
   * @return the departure time in seconds at the given stop
   * @throws IndexOutOfBoundsException if stopIndex is not found
   */
  default int departure(int startStopPos, int stopIndex) {
    return departure(pattern().findStopPositionAfter(startStopPos, stopIndex));
  }

  /// The relative-travel-duration is a proxy for time spent on transit from the boarding stop to
  /// the alight stop. We do not know the alight stop, so it is impossible to calculate the
  /// "correct" time. The only thing that matters is that the relative difference between two
  /// boardings is correct. Compute a relative-time that can be used to compare the travel-time
  /// cost for any two boardings in the same pattern.
  ///
  /// Two invariants must hold:
  ///
  /// - For two boardings at stop positions `i` and `j` on the same trip (where `i` comes before
  ///   `j` in the pattern), `relativeTravelDuration(boardAtI) - relativeTravelDuration(boardAtJ)`
  ///   must equal the actual transit duration from stop `i` to stop `j`. If the board time at
  ///   position 3 is 10:00 and at the next stop is 10:05, then the value at position 3 is larger
  ///   by 5*60s = 300 than at the next stop.
  /// - All trips in the same pattern should return the same value for at least one stop. If you
  ///   choose to anchor the relative duration to the last stop, and the value for one trip at the
  ///   last stop is 56_000, then it must be 56_000 at the last stop for all other trips in the
  ///   same pattern.
  int relativeTravelDuration(int boardTime);

  /**
   * Return the pattern for this trip.
   */
  RaptorTripPattern pattern();

  /**
   * Search for the arrival stop position for the given trip, latest arrival time, and stop index.
   * We need the time in addition to the stop in cases where the trip pattern visits the same stop
   * twice. Also, the time alone is not sufficient, since more than one stop could have the exact
   * same arrival time.
   * <p>
   * Raptor saves memory by NOT storing board/alight stop positions in the pattern; therefore, we
   * need this method when mapping to an itinerary or Raptor path.
   * <p>
   * Avoid using this during routing, as it is not optimized for performance.
   *
   * @return the stop position in the trip pattern if found; otherwise, -1
   */
  default int findArrivalStopPosition(int latestArrivalTime, int stop) {
    RaptorTripPattern p = pattern();
    int i = p.numberOfStopsInPattern() - 1;

    while (arrival(i) > latestArrivalTime) {
      --i;
      if (i == -1) {
        return -1;
      }
    }
    return p.findStopPositionBefore(i, stop);
  }

  /**
   * Search for the departure stop position for the given trip, earliest departure time, and stop
   * index. We need the time in addition to the stop in cases where the trip pattern visits the
   * same stop twice. Also, the time alone is not sufficient, since more than one stop could have
   * the exact same departure time.
   * <p>
   * Raptor saves memory by NOT storing board/alight stop positions in the pattern; therefore, we
   * need this method when mapping to an itinerary or Raptor path.
   * <p>
   * Avoid using this during routing, as it is not optimized for performance.
   *
   * @return the stop position in the trip pattern if found; otherwise, -1
   */
  default int findDepartureStopPosition(int earliestDepartureTime, int stop) {
    var p = pattern();
    final int size = p.numberOfStopsInPattern();
    int i = 0;

    while (departure(i) < earliestDepartureTime) {
      ++i;
      if (i == size) {
        return -1;
      }
    }
    return p.findStopPositionAfter(i, stop);
  }

  /**
   * Find all departure stop positions for a stop index after the given earliest departure time.
   * This is useful because a trip can pass through the same stop more than once if the stop pattern
   * is circular. This method returns all stop positions, while
   * {@link #findDepartureStopPosition} returns only the first stop position found.
   *
   * @return list of all valid stop positions for a given stop index
   */
  default List<Integer> findDepartureStopPositions(int earliestDepartureTime, int stop) {
    var p = pattern();
    final int size = p.numberOfStopsInPattern();
    int i = 0;

    while (departure(i) < earliestDepartureTime) {
      ++i;
      if (i == size) {
        return new ArrayList<>();
      }
    }

    var stops = new ArrayList<Integer>();

    while (i < size) {
      if (stop == p.stopIndex(i)) {
        stops.add(i);
      }

      i++;
    }

    return stops;
  }
}
