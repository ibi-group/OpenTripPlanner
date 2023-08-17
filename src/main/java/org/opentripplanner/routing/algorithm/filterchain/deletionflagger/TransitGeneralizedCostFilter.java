package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * This filter remove all transit results which have a generalized-cost higher than the max-limit
 * computed by the {@link #costLimitFunction} plus the wait cost given by
 * {@link TransitGeneralizedCostFilter#getWaitTimeCost}.
 */
public class TransitGeneralizedCostFilter implements ItineraryDeletionFlagger {

  private final CostLinearFunction costLimitFunction;

  private final double intervalRelaxFactor;

  public TransitGeneralizedCostFilter(CostLinearFunction costLimit, double intervalRelaxFactor) {
    this.costLimitFunction = costLimit;
    this.intervalRelaxFactor = intervalRelaxFactor;
  }

  @Override
  public String name() {
    return "transit-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    List<Itinerary> transitItineraries = itineraries
      .stream()
      .filter(Itinerary::hasTransit)
      .sorted(Comparator.comparingInt(Itinerary::getGeneralizedCost))
      .toList();

    return transitItineraries
      .stream()
      .filter(it -> transitItineraries.stream().anyMatch(t -> acceptGeneralizedCost(it, t)))
      .collect(Collectors.toList());
  }

  private boolean acceptGeneralizedCost(Itinerary subject, Itinerary transitItinerary) {
    return subject.getGeneralizedCost() > calculateLimit(subject, transitItinerary);
  }

  private int calculateLimit(Itinerary subject, Itinerary transitItinerary) {
    return (
      costLimitFunction
        .calculate(Cost.costOfSeconds(transitItinerary.getGeneralizedCost()))
        .toSeconds() +
      getWaitTimeCost(transitItinerary, subject)
    );
  }

  private int getWaitTimeCost(Itinerary a, Itinerary b) {
    return IntUtils.round(
      intervalRelaxFactor *
      Math.min(
        Math.abs(ChronoUnit.SECONDS.between(a.startTime(), b.startTime())),
        Math.abs(ChronoUnit.SECONDS.between(a.endTime(), b.endTime()))
      )
    );
  }
}
