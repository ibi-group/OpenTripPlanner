package org.opentripplanner.model.plan;

import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.CAR;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;
import static org.opentripplanner.street.search.TraverseMode.WALK;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Calculate derived itinerary fields
 */
class ItinerariesCalculateLegTotals {

  int nTransitLegs = 0;
  double nonTransitDistanceMeters = 0.0;
  double walkDistanceMeters = 0.0;
  boolean walkOnly = true;
  boolean streetOnly = true;
  double totalElevationGained = 0.0;
  double totalElevationLost = 0.0;
  private ItineraryDurations durations;

  public ItinerariesCalculateLegTotals(List<Leg> legs) {
    if (legs.isEmpty()) {
      return;
    }
    calculate(legs);
  }

  int transfers() {
    return nTransitLegs == 0 ? 0 : nTransitLegs - 1;
  }

  private void calculate(List<Leg> legs) {
    var totalDuration = Duration.between(
      legs.getFirst().getStartTime(),
      legs.getLast().getEndTime()
    );
    Multimap<TraverseMode, Duration> nonTransitDurations = ArrayListMultimap.create();
    Duration transitDuration = Duration.ZERO;

    for (Leg leg : legs) {
      Duration dt = leg.getDuration();

      if (leg.isTransitLeg()) {
        transitDuration = transitDuration.plus(dt);
        if (!leg.isInterlinedWithPreviousLeg()) {
          ++nTransitLegs;
        }
      } else if (leg instanceof StreetLeg streetLeg) {
        nonTransitDurations.put(streetLeg.getMode(), streetLeg.getDuration());
        nonTransitDistanceMeters += leg.getDistanceMeters();

        if (streetLeg.isWalkingLeg()) {
          walkDistanceMeters = walkDistanceMeters + streetLeg.getDistanceMeters();
        }
      } else if (leg instanceof UnknownTransitPathLeg unknownTransitPathLeg) {
        nTransitLegs += unknownTransitPathLeg.getNumberOfTransfers() + 1;
      }

      if (!leg.isWalkingLeg()) {
        walkOnly = false;
      }

      if (!leg.isStreetLeg()) {
        this.streetOnly = false;
      }

      if (leg.getElevationProfile() != null) {
        var p = leg.getElevationProfile();
        this.totalElevationGained += p.elevationGained();
        this.totalElevationLost += p.elevationLost();
      }
    }
    var nonTransitDuration = sum(nonTransitDurations.values());
    var waitingDuration = totalDuration.minus(transitDuration).minus(nonTransitDuration);

    this.durations =
      new ItineraryDurations(
        sum(nonTransitDurations.get(WALK)),
        sum(nonTransitDurations.get(BICYCLE)),
        sum(nonTransitDurations.get(SCOOTER)),
        sum(nonTransitDurations.get(CAR)),
        waitingDuration,
        nonTransitDuration,
        transitDuration,
        totalDuration
      );
  }

  ItineraryDurations durations() {
    return durations;
  }

  private Duration sum(Collection<Duration> values) {
    return values.stream().reduce(Duration.ZERO, Duration::plus);
  }

  record ItineraryDurations(
    Duration walk,
    Duration bicycle,
    Duration scooter,
    Duration car,
    Duration waiting,
    Duration nonTransit,
    Duration transit,
    Duration total
  ) {}
}
