package org.opentripplanner.api.mapping;

import java.util.Locale;
import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.model.plan.TripPlan;

public class TripPlanMapper {

  private final ItineraryMapper itineraryMapper;
  private final PlaceMapper placeMapper;

  public TripPlanMapper(
    Locale locale,
    boolean addIntermediateStops,
    boolean addAccessibilityScore
  ) {
    this.itineraryMapper = new ItineraryMapper(locale, addIntermediateStops, addAccessibilityScore);
    this.placeMapper = new PlaceMapper(locale);
  }

  public ApiTripPlan mapTripPlan(TripPlan domain) {
    if (domain == null) {
      return null;
    }
    ApiTripPlan api = new ApiTripPlan();
    api.date = domain.date;
    // The origin/destination do not have arrival/depature times; Hence {@code null} is used.
    api.from = placeMapper.mapPlace(domain.from, null, null, null, null);
    api.to = placeMapper.mapPlace(domain.to, null, null, null, null);
    api.itineraries = itineraryMapper.mapItineraries(domain.itineraries);
    return api;
  }
}
