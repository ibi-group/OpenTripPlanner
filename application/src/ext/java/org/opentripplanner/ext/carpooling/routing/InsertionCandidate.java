package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Represents a viable insertion of a passenger into a carpool trip.
 * <p>
 * Contains all information needed to construct an itinerary, including:
 * - The original trip
 * - Insertion positions (where pickup and dropoff occur in the modified route)
 * - Route segments (all GraphPaths forming the complete modified route)
 * - Timing information
 * <p>
 * {@code pickupPosition} and {@code dropoffPosition} are 0-based indices of the passenger's
 * pickup and dropoff stops in the modified route (the route after the passenger's stops have
 * been inserted into the carpool trip).
 */
public record InsertionCandidate(
  CarpoolTrip trip,
  int pickupPosition,
  int dropoffPosition,
  List<GraphPath<State, Edge, Vertex>> routeSegments,
  Duration stopDuration,
  NearbyStop transitStop,
  Duration totalTripDuration
) {
  public InsertionCandidate(
    CarpoolTrip trip,
    int pickupPosition,
    int dropoffPosition,
    List<GraphPath<State, Edge, Vertex>> routeSegments,
    Duration stopDuration,
    NearbyStop transitStop
  ) {
    this(
      trip,
      pickupPosition,
      dropoffPosition,
      routeSegments,
      stopDuration,
      transitStop,
      computeTotalTripDuration(routeSegments, stopDuration)
    );
  }

  private static Duration computeTotalTripDuration(
    List<GraphPath<State, Edge, Vertex>> routeSegments,
    Duration stopDuration
  ) {
    Duration[] cumulativeDurations = GraphPathUtils.calculateCumulativeDurations(
      routeSegments.toArray(new GraphPath[0]),
      stopDuration
    );
    return cumulativeDurations[cumulativeDurations.length - 1];
  }

  /**
   * Gets the pickup route segment(s) - from boarding to passenger pickup.
   * Returns all segments before the pickup position.
   */
  public List<GraphPath<State, Edge, Vertex>> getPickupSegments() {
    if (pickupPosition == 0) {
      return List.of();
    }
    return routeSegments.subList(0, pickupPosition);
  }

  /**
   * Gets the shared route segment(s) - from passenger pickup to dropoff.
   * Returns all segments between pickup and dropoff positions.
   */
  public List<GraphPath<State, Edge, Vertex>> getSharedSegments() {
    return routeSegments.subList(pickupPosition, dropoffPosition);
  }

  /**
   * Gets the dropoff route segment(s) - from passenger dropoff to alighting.
   * Returns all segments after the dropoff position.
   */
  public List<GraphPath<State, Edge, Vertex>> getDropoffSegments() {
    if (dropoffPosition >= routeSegments.size()) {
      return List.of();
    }
    return routeSegments.subList(dropoffPosition, routeSegments.size());
  }

  /**
   * Calculates the duration from trip start until the car arrives at the passenger's pickup.
   * Includes travel time through pickup segments and intermediate stop delays between them, but
   * <em>excludes</em> the boarding dwell at the pickup itself — that is accounted for in
   * {@link #getPassengerRideDuration()}.
   * Returns {@link Duration#ZERO} when the passenger boards at the trip origin (no pickup segments).
   */
  public Duration getDurationUntilPickupArrival() {
    return totalSegmentDuration(getPickupSegments(), stopDuration);
  }

  /**
   * Calculates the duration of the passenger's ride from pickup arrival to dropoff.
   * Includes the boarding dwell at the pickup (when there are pickup segments preceding it),
   * travel time through shared segments, and stop delays at intermediate stops between shared
   * segments. The no-pickup-segments case (passenger boarding at the trip origin) cannot occur
   * today — the search never places {@code pickupPosition == 0} — but the branch guards against
   * it by omitting the boarding dwell.
   */
  public Duration getPassengerRideDuration() {
    Duration boardingDwell = pickupPosition == 0 ? Duration.ZERO : stopDuration;
    return totalSegmentDuration(getSharedSegments(), stopDuration).plus(boardingDwell);
  }

  private static Duration totalSegmentDuration(
    List<GraphPath<State, Edge, Vertex>> segments,
    Duration stopDuration
  ) {
    return segments
      .stream()
      .map(GraphPathUtils::calculateDuration)
      .reduce(Duration.ZERO, Duration::plus)
      .plus(stopDuration.multipliedBy(Math.max(0, segments.size() - 1)));
  }

  @Override
  public String toString() {
    return String.format(
      "InsertionCandidate{trip=%s, pickup@%d, dropoff@%d, duration=%ds, segments=%d}",
      trip.getId(),
      pickupPosition,
      dropoffPosition,
      totalTripDuration.getSeconds(),
      routeSegments.size()
    );
  }
}
