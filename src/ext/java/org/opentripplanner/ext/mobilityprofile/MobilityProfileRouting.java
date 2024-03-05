package org.opentripplanner.ext.mobilityprofile;

import static java.util.Map.entry;

import java.util.EnumMap;
import java.util.Map;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class MobilityProfileRouting {

  public static final String HIGHWAY_TAG = "highway";

  public static final String FOOTWAY_TAG_VALUE = "footway";

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

  /** Computes the travel time, in minutes, for the given distance and mobility profile. */
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

  public static boolean isHighwayFootway(OSMWay way) {
    return way.hasTag(HIGHWAY_TAG) && FOOTWAY_TAG_VALUE.equals(way.getTag(HIGHWAY_TAG));
  }

  public static StreetTraversalPermission adjustPedestrianPermissions(
    OSMWay way,
    StreetTraversalPermission permissions
  ) {
    return isHighwayFootway(way) ? permissions : permissions.remove(StreetTraversalPermission.PEDESTRIAN);
  }
}
