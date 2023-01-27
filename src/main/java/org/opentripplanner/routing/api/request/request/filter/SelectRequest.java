package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class SelectRequest implements Serializable {

  public static Builder of() {
    return new Builder();
  }

  private final List<MainAndSubMode> transportModes;
  private final AllowTransitModeFilter transportModeFilter;
  private final List<FeedScopedId> agencies;
  private final RouteMatcher routes;

  // TODO: 2022-11-29 filters: group of routes

  public SelectRequest(Builder builder) {
    if (!builder.transportModes.isEmpty()) {
      this.transportModeFilter = AllowTransitModeFilter.of(builder.transportModes);
    } else {
      this.transportModeFilter = null;
    }

    // TODO: 2022-12-20 filters: having list of modes and modes filter in same instance is not very elegant
    this.transportModes = builder.transportModes;

    this.agencies = Collections.unmodifiableList(builder.agencies);
    this.routes = builder.routes;
  }

  public boolean matches(Route route) {
    if (
      this.transportModeFilter != null &&
      !this.transportModeFilter.match(route.getMode(), route.getNetexSubmode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(route.getAgency().getId())) {
      return false;
    }

    if (!routes.isEmpty() && !routes.matches(route)) {
      return false;
    }

    return true;
  }

  public boolean matches(TripTimes tripTimes) {
    var trip = tripTimes.getTrip();

    if (
      this.transportModeFilter != null &&
      !this.transportModeFilter.match(trip.getMode(), trip.getNetexSubMode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(trip.getRoute().getAgency().getId())) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(SelectRequest.class)
      .addObj("transportModes", transportModes)
      .addCol("agencies", agencies)
      .addObj("routes", routes)
      .toString();
  }

  public List<MainAndSubMode> transportModes() {
    return transportModes;
  }

  public AllowTransitModeFilter transportModeFilter() {
    return transportModeFilter;
  }

  public List<FeedScopedId> agencies() {
    return agencies;
  }

  public RouteMatcher routes() {
    return this.routes;
  }

  public static class Builder {

    private List<MainAndSubMode> transportModes = new ArrayList<>();
    private List<FeedScopedId> agencies = new ArrayList<>();
    private RouteMatcher routes = RouteMatcher.emptyMatcher();

    // TODO: 2022-11-29 filters: group of routes

    public Builder withTransportModes(List<MainAndSubMode> transportModes) {
      this.transportModes = transportModes;

      return this;
    }

    public Builder addTransportMode(MainAndSubMode transportMode) {
      this.transportModes.add(transportMode);

      return this;
    }

    public Builder withAgenciesFromString(String s) {
      if (!s.isEmpty()) {
        this.agencies = FeedScopedId.parseListOfIds(s);
      }

      return this;
    }

    public Builder withAgencies(List<FeedScopedId> agencies) {
      this.agencies = agencies;

      return this;
    }

    public Builder withRoutesFromString(String s) {
      if (!s.isEmpty()) {
        this.routes = RouteMatcher.parse(s);
      } else {
        this.routes = RouteMatcher.emptyMatcher();
      }

      return this;
    }

    public Builder withRoutes(RouteMatcher routes) {
      this.routes = routes;

      return this;
    }

    public SelectRequest build() {
      return new SelectRequest(this);
    }
  }
}
