package org.opentripplanner.ext.carpooling.internal;

import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Maps carpooling insertion candidates to OTP itineraries for API responses.
 * <p>
 * This mapper bridges between the carpooling domain model ({@link InsertionCandidate}) and
 * OTP's standard itinerary model ({@link Itinerary}). It extracts the passenger's journey
 * portion from the complete driver route and constructs an itinerary with timing, geometry,
 * and cost information.
 *
 * <h2>Mapping Strategy</h2>
 * <p>
 * An {@link InsertionCandidate} contains:
 * <ul>
 *   <li><strong>Pickup segments:</strong> Driver's route from start to passenger pickup</li>
 *   <li><strong>Shared segments:</strong> Passenger's journey from pickup to dropoff</li>
 *   <li><strong>Dropoff segments:</strong> Driver's route from dropoff to end</li>
 * </ul>
 * <p>
 * This mapper focuses on the <strong>shared segments</strong>, which represent the passenger's
 * actual carpool ride. The pickup segments are used only to calculate when the driver arrives
 * at the pickup location.
 *
 * <h2>Time Calculation</h2>
 * <p>
 * The passenger's start time is the moment the driver arrives at the pickup location
 * (trip start + pickup travel); the boarding dwell is included in the leg's duration, not
 * added before it. The start time is <em>not</em> shifted to match the passenger's requested
 * departure time: the driver is on a committed schedule and cannot wait. Whether the
 * passenger should show up early, or whether a trip starting before the requested time
 * should be matched at all, is a filtering concern and lives upstream of this mapper.
 *
 * <h2>Geometry and Cost</h2>
 * <p>
 * The itinerary includes:
 * <ul>
 *   <li><strong>Geometry:</strong> Concatenated line strings from all shared route edges</li>
 *   <li><strong>Distance:</strong> Sum of all shared segment edge distances</li>
 *   <li><strong>Generalized cost:</strong> A* path weight from routing (time + penalties)</li>
 * </ul>
 *
 * <h2>Package Location</h2>
 * <p>
 * This class is in the {@code internal} package because it's an implementation detail of
 * the carpooling service. API consumers interact with {@link Itinerary} objects, not this mapper.
 *
 * @see InsertionCandidate for the source data structure
 * @see CarpoolLeg for the carpool-specific leg type
 * @see Itinerary for the OTP itinerary model
 */
public class CarpoolItineraryMapper {

  private final ZonedDateTime transitSearchTimeZero;

  /**
   * @param transitSearchTimeZero the base time for access egress requests; not used for direct
   */
  public CarpoolItineraryMapper(ZonedDateTime transitSearchTimeZero) {
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  public CarpoolItineraryMapper() {
    this(null);
  }

  /**
   * Converts an insertion candidate into an OTP itinerary representing the passenger's journey.
   * <p>
   * Extracts the passenger's portion of the journey (shared segments) and constructs an itinerary
   * with accurate timing, geometry, and cost information. The resulting itinerary contains a
   * single {@link CarpoolLeg} representing the ride from pickup to dropoff.
   *
   * <h3>Time Calculation Details</h3>
   * <p>
   * Start and end times come entirely from the driver's schedule:
   * <ol>
   *   <li><strong>Start:</strong> {@code trip.startTime() +}
   *       {@link InsertionCandidate#getDurationUntilPickupArrival()} — the moment the driver
   *       arrives at the pickup point.</li>
   *   <li><strong>End:</strong> {@code start +}
   *       {@link InsertionCandidate#getPassengerRideDuration()}, which already includes the
   *       boarding dwell at the pickup and any intermediate stop delays along the shared
   *       segments.</li>
   * </ol>
   *
   * <h3>Null Return Cases</h3>
   * <p>
   * Returns {@code null} if the candidate has no shared segments, which should never happen
   * for valid insertion candidates but serves as a safety check.
   *
   * @param candidate the insertion candidate containing route segments and trip details
   * @return an itinerary with a single carpool leg, or null if shared segments are empty
   *         (should not occur for valid candidates)
   */
  @Nullable
  public Itinerary toItinerary(InsertionCandidate candidate) {
    var sharedSegments = candidate.getSharedSegments();
    if (sharedSegments.isEmpty()) {
      return null;
    }

    var startTime = candidate.trip().startTime().plus(candidate.getDurationUntilPickupArrival());

    var endTime = startTime.plus(candidate.getPassengerRideDuration());

    var firstSegment = sharedSegments.getFirst();
    var lastSegment = sharedSegments.getLast();

    Vertex fromVertex = firstSegment.states.getFirst().getVertex();
    Vertex toVertex = lastSegment.states.getLast().getVertex();

    var allEdges = sharedSegments
      .stream()
      .flatMap(seg -> seg.edges.stream())
      .toList();

    CarpoolLeg carpoolLeg = CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, new NonLocalizedString("Carpool boarding")))
      .withTo(Place.normal(toVertex, new NonLocalizedString("Carpool alighting")))
      .withGeometry(GeometryUtils.concatenateLineStrings(allEdges, Edge::getGeometry))
      .withDistanceMeters(allEdges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) lastSegment.getWeight())
      .build();

    return Itinerary.ofDirect(List.of(carpoolLeg))
      .withGeneralizedCost(Cost.costOfSeconds(carpoolLeg.generalizedCost()))
      .build();
  }

  public Itinerary toItinerary(CarpoolAccessEgress accessEgress) {
    var segments = accessEgress.getSegments();
    var allEdges = segments
      .stream()
      .flatMap(seg -> seg.edges.stream())
      .toList();
    var startTime = transitSearchTimeZero.plusSeconds(accessEgress.getDepartureTimeOfPassenger());
    var endTime = transitSearchTimeZero.plusSeconds(accessEgress.getArrivalTimeOfPassenger());
    var fromVertex = segments.getFirst().states.getFirst().getVertex();
    var toVertex = segments.getLast().states.getLast().getVertex();
    LineString geometry = GeometryUtils.concatenateLineStrings(allEdges, Edge::getGeometry);
    var cost = accessEgress.getTotalWeight();

    var carpoolLeg = CarpoolLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withFrom(Place.normal(fromVertex, new NonLocalizedString("Carpool boarding")))
      .withTo(Place.normal(toVertex, new NonLocalizedString("Carpool alighting")))
      .withDistanceMeters(allEdges.stream().mapToDouble(Edge::getDistanceMeters).sum())
      .withGeneralizedCost((int) cost)
      .withGeometry(geometry)
      .build();

    var itinerary = Itinerary.ofDirect(List.of(carpoolLeg))
      .withGeneralizedCost(Cost.costOfSeconds(carpoolLeg.generalizedCost()))
      .build();

    return itinerary;
  }
}
