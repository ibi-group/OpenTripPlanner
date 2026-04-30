package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Interface for filtering carpool trips before expensive routing calculations.
 * <p>
 * Filters are applied as a pre-screening mechanism to quickly eliminate
 * incompatible trips based on various criteria (capacity, time, distance, etc.).
 * <p>
 * Supports both direct routing (pickup + dropoff) and access/egress routing
 * (single passenger coordinate near a transit stop).
 */
public interface TripFilter {
  /**
   * Checks if a trip passes this filter for the given passenger request.
   *
   * @param trip The carpool trip to evaluate
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @return true if the trip passes the filter, false otherwise
   */
  boolean accepts(CarpoolTrip trip, WgsCoordinate passengerPickup, WgsCoordinate passengerDropoff);

  /**
   * Checks if a trip passes this filter for the given passenger request with time information.
   * <p>
   * Default implementation delegates to the simpler {@link #accepts(CarpoolTrip, WgsCoordinate, WgsCoordinate)}
   * method, ignoring the time parameter. Time-aware filters should override this method.
   *
   * @param trip The carpool trip to evaluate
   * @param passengerPickup Passenger's pickup location
   * @param passengerDropoff Passenger's dropoff location
   * @param passengerDepartureTime Passenger's requested departure time
   * @param searchWindow Time window around the requested departure time
   * @return true if the trip passes the filter, false otherwise
   */
  default boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    // Default: ignore time and delegate to coordinate-only method
    return accepts(trip, passengerPickup, passengerDropoff);
  }

  /**
   * Checks if a trip passes this filter for access/egress routing.
   * <p>
   * Used when evaluating carpool trip viability for connecting passengers
   * to public transit stops. Default implementation always returns true.
   *
   * @param trip Carpool trip
   * @param coordinateOfPassenger Coordinates of origin if access, and destination if egress
   * @param passengerDepartureTime Requested departure time of the passenger
   * @param searchWindow The time window around the requested departure time
   * @return true if the filter passes, false if it doesn't
   */
  default boolean acceptsAccessEgress(
    CarpoolTrip trip,
    WgsCoordinate coordinateOfPassenger,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    return true;
  }
}
