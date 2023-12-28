package org.opentripplanner.ext.mobilityprofile;

/**
 * Enumeration for the mobility profiles, and their associated column names for CSV parsing.
 */
public enum MobilityProfile {
  NONE("None"),
  SOME("Some"),
  DEVICE("Device"),
  WCHAIRM("WChairM"),
  WCHAIRE("WChairE"),
  MSCOOTER("MScooter"),
  VISION("Vision"),
  VISIONPLUS("Vision+"),
  SOME_VISION("Some-Vision"),
  DEVICE_VISION("Device-Vision"),
  WCHAIRM_VISION("WChairM-Vision"),
  WCHAIRE_VISION("WChairE-Vision"),
  MSCOOTER_VISION("MScooter-Vision"),
  SOME_VISIONPLUS("Some-Vision+"),
  DEVICE_VISIONPLUS("Device-Vision+"),
  WCHAIRM_VISIONPLUS("WChairM-Vision+"),
  WCHAIRE_VISIONPLUS("WChairE-Vision+"),
  MSCOOTER_VISIONPLUS("MScooter-Vision+");

  private final String text;

  MobilityProfile(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return text;
  }

  public static MobilityProfile fromString(String value) {
    for (MobilityProfile p : MobilityProfile.values()) {
      if (p.text.equals(value)) {
        return p;
      }
    }
    throw new RuntimeException(String.format("Invalid mobility profile '%s'", value));
  }
}
