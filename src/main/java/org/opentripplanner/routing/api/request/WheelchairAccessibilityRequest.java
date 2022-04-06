package org.opentripplanner.routing.api.request;

public record WheelchairAccessibilityRequest(
  boolean enabled,
  WheelchairAccessibilityFeature trips,
  WheelchairAccessibilityFeature stops
) {
  public static final WheelchairAccessibilityRequest DEFAULTS = new WheelchairAccessibilityRequest(
    false,
    new WheelchairAccessibilityFeature(true, 600, 3600),
    new WheelchairAccessibilityFeature(true, 600, 3600)
  );

  public static WheelchairAccessibilityRequest makeDefault(boolean enabled) {
    return new WheelchairAccessibilityRequest(enabled, DEFAULTS.trips, DEFAULTS.stops);
  }

  public WheelchairAccessibilityRequest withEnabled(boolean enabled) {
    return new WheelchairAccessibilityRequest(enabled, this.trips, this.stops);
  }
}
