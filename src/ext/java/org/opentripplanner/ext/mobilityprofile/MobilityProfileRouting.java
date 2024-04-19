package org.opentripplanner.ext.mobilityprofile;

import static java.util.Map.entry;

import java.util.EnumMap;
import java.util.Map;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;

public class MobilityProfileRouting {

  private static final Map<MobilityProfile, Float> TRAVEL_SPEED_MPH_BY_PROFILE = new EnumMap<>(
    Map.ofEntries(
      entry(MobilityProfile.NONE, 2.5f),
      entry(MobilityProfile.SOME, 2.0f),
      entry(MobilityProfile.DEVICE, 1.5f),
      entry(MobilityProfile.WCHAIRM, 3.0f),
      entry(MobilityProfile.WCHAIRE, 4.0f),
      entry(MobilityProfile.MSCOOTER, 5.0f),
      entry(MobilityProfile.LOW_VISION, 2.0f),
      entry(MobilityProfile.BLIND, 1.5f),
      entry(MobilityProfile.SOME_LOW_VISION, 2.0f),
      entry(MobilityProfile.DEVICE_LOW_VISION, 1.5f),
      entry(MobilityProfile.WCHAIRM_LOW_VISION, 2.0f),
      entry(MobilityProfile.WCHAIRE_LOW_VISION, 2.0f),
      entry(MobilityProfile.MSCOOTER_LOW_VISION, 2.0f),
      entry(MobilityProfile.SOME_BLIND, 2.0f),
      entry(MobilityProfile.DEVICE_BLIND, 1.5f),
      entry(MobilityProfile.WCHAIRM_BLIND, 2.0f),
      entry(MobilityProfile.WCHAIRE_BLIND, 1.5f),
      entry(MobilityProfile.MSCOOTER_BLIND, 2.0f)
    )
  );

  public static final double ONE_MILE_IN_KILOMETERS = 1.609;

  private MobilityProfileRouting() {
    // Np public constructor.
  }

  /** Computes the travel time, in hours, for the given distance and mobility profile. */
  public static float computeTravelHours(double meters, MobilityProfile mobilityProfile) {
    return (float) (
      meters /
      1000 /
      ONE_MILE_IN_KILOMETERS /
      TRAVEL_SPEED_MPH_BY_PROFILE.getOrDefault(
        mobilityProfile,
        TRAVEL_SPEED_MPH_BY_PROFILE.get(MobilityProfile.NONE)
      )
    );
  }

  public static StreetTraversalPermission adjustPedestrianPermissions(
    OSMWay way,
    StreetTraversalPermission permissions
  ) {
    return way.isFootway()
      ? permissions
      : permissions.remove(StreetTraversalPermission.PEDESTRIAN);
  }

  /** Multiplies profile costs by the distance ratio between the given edge and its parent. */
  public static Map<MobilityProfile, Float> getProRatedProfileCosts(
    TemporaryPartialStreetEdge tmpEdge
  ) {
    StreetEdge parentEdge = tmpEdge.getParentEdge();
    float ratio = (float) (tmpEdge.getDistanceMeters() / parentEdge.getDistanceMeters());
    return getProRatedProfileCosts(parentEdge.profileCost, ratio);
  }

  public static Map<MobilityProfile, Float> getProRatedProfileCosts(
    Map<MobilityProfile, Float> cost,
    float ratio
  ) {
    // Has to be a HashMap for graph serialization
    Map<MobilityProfile, Float> result = new EnumMap<>(MobilityProfile.class);
    cost.forEach((k, v) -> result.put(k, v * ratio));
    return result;
  }
}
