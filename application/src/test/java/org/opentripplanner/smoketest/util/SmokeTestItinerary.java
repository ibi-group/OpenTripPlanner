package org.opentripplanner.smoketest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.client.model.Itinerary;
import org.opentripplanner.client.model.Leg;
import org.opentripplanner.client.model.TripPlan;

public class SmokeTestItinerary {

  private final List<LegMatcher> legMatchers = new ArrayList<>();
  private final TripPlan tripPlan;

  public SmokeTestItinerary(TripPlan tripPlan) {
    this.tripPlan = tripPlan;
  }

  public static SmokeTestItinerary from(TripPlan tripPlan) {
    return new SmokeTestItinerary(tripPlan);
  }

  public SmokeTestItinerary hasLeg() {
    legMatchers.add(new LegMatcher());
    return this;
  }

  public SmokeTestItinerary withRouteShortName(String shortName) {
    getCurrentMatcher().routeShortName = shortName;
    return this;
  }

  public SmokeTestItinerary withFarePrice(float price) {
    getCurrentMatcher().farePrice = price;
    return this;
  }

  public SmokeTestItinerary withMode(String mode) {
    getCurrentMatcher().mode = mode;
    return this;
  }

  public void assertMatches() {
    for (LegMatcher matcher : legMatchers) {
      List<PartialMatch> partialMatches = new ArrayList<>();
      boolean foundMatch = false;

      for (Itinerary itinerary : tripPlan.itineraries()) {
        for (Leg leg : itinerary.legs()) {
          MatchResult result = matcher.matches(leg);
          if (result.isFullMatch()) {
            foundMatch = true;
            break;
          } else if (result.hasAnyMatch()) {
            partialMatches.add(new PartialMatch(leg, result));
          }
        }
        if (foundMatch) break;
      }

      if (!foundMatch) {
        StringBuilder error = new StringBuilder("No leg found matching all criteria:\n");
        error.append(matcher.describeCriteria()).append("\n\n");

        if (!partialMatches.isEmpty()) {
          error.append("Partial matches found:\n");
          for (PartialMatch match : partialMatches) {
            error
              .append("- Leg with ")
              .append(match.result.getMatchingCriteria())
              .append(" but missing ")
              .append(match.result.getMissingCriteria())
              .append("\n");
          }
        }
        throw new AssertionError(error.toString());
      }
    }
  }

  private LegMatcher getCurrentMatcher() {
    if (legMatchers.isEmpty()) {
      throw new IllegalStateException("Call hasLeg() first before adding criteria");
    }
    return legMatchers.get(legMatchers.size() - 1);
  }

  private static class MatchResult {

    private final Map<String, Boolean> criteria = new HashMap<>();

    void addCriterion(String name, boolean matched) {
      criteria.put(name, matched);
    }

    boolean isFullMatch() {
      return criteria.values().stream().allMatch(v -> v);
    }

    boolean hasAnyMatch() {
      return criteria.values().stream().anyMatch(v -> v);
    }

    String getMatchingCriteria() {
      return criteria
        .entrySet()
        .stream()
        .filter(Map.Entry::getValue)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(", "));
    }

    String getMissingCriteria() {
      return criteria
        .entrySet()
        .stream()
        .filter(e -> !e.getValue())
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(", "));
    }
  }

  private static class PartialMatch {

    private final Leg leg;
    private final MatchResult result;

    PartialMatch(Leg leg, MatchResult result) {
      this.leg = leg;
      this.result = result;
    }
  }

  private static class LegMatcher {

    String routeShortName;
    float farePrice;
    String mode;

    MatchResult matches(Leg leg) {
      MatchResult result = new MatchResult();

      if (routeShortName != null) {
        boolean matches =
          leg.isTransit() &&
          leg.route().shortName().isPresent() &&
          leg.route().shortName().get().equals(routeShortName);
        result.addCriterion("route '" + routeShortName + "'", matches);
      }

      if (farePrice > 0) {
        boolean matches = leg
          .fareProducts()
          .stream()
          .anyMatch(fp -> fp.product().price().amount().floatValue() == farePrice);
        result.addCriterion("fare $" + farePrice, matches);
      }

      if (mode != null) {
        boolean matches = leg.mode().toString().equals(mode);
        result.addCriterion("mode " + mode, matches);
      }

      return result;
    }

    String describeCriteria() {
      List<String> criteria = new ArrayList<>();
      if (routeShortName != null) criteria.add("route '" + routeShortName + "'");
      if (farePrice > 0) criteria.add("fare $" + farePrice);
      if (mode != null) criteria.add("mode " + mode);
      return String.join(", ", criteria);
    }
  }
}
