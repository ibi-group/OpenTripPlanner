package org.opentripplanner.model.plan;

import java.util.List;
import java.util.stream.Collectors;

public class ItinerarySummarizer {

  private final List<Itinerary> itins;

  public ItinerarySummarizer(List<Itinerary> itins) {
    this.itins = itins;
  }

  public List<String> summarize() {
    return itins.stream().map(this::summarize).toList();
  }

  private String summarize(Itinerary leg) {
    return leg.legs().stream().map(this::summarizeLeg).collect(Collectors.joining(","));
  }

  private String summarizeLeg(Leg leg) {
    return switch (leg) {
      case TransitLeg tl -> summarizeTransitLeg(tl);
      default -> throw new IllegalStateException("Unexpected value: " + leg);
    };
  }

  private static String summarizeTransitLeg(TransitLeg leg) {
    StringBuilder result = new StringBuilder();
    result.append(leg.trip().getId());
    result.append(" ");
    result.append(leg.startTime().toLocalTime());
    result.append("[");
    result.append(leg.from().stop.getId());
    result.append("]");
    result.append(" → ");
    result.append(leg.endTime().toLocalTime());
    result.append("[");
    result.append(leg.to().stop.getId());
    result.append("]");
    return result.toString();
  }
}
