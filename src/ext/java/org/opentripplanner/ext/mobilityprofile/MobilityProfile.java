package org.opentripplanner.ext.mobilityprofile;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
  LOW_VISION("LowVision"),
  BLIND("Blind"),
  SOME_LOW_VISION("Some-LowVision"),
  DEVICE_LOW_VISION("Device-LowVision"),
  WCHAIRM_LOW_VISION("WChairM-LowVision"),
  WCHAIRE_LOW_VISION("WChairE-LowVision"),
  MSCOOTER_LOW_VISION("MScooter-LowVision"),
  SOME_BLIND("Some-Blind"),
  DEVICE_BLIND("Device-Blind"),
  WCHAIRM_BLIND("WChairM-Blind"),
  WCHAIRE_BLIND("WChairE-Blind"),
  MSCOOTER_BLIND("MScooter-Blind");

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
    if (isBlank(value)) return null;

    for (MobilityProfile p : MobilityProfile.values()) {
      if (p.text.equals(value)) {
        return p;
      }
    }

    throw new RuntimeException(String.format("Invalid mobility profile '%s'", value));
  }
}
