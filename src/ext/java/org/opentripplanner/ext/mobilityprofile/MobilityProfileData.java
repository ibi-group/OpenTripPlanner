package org.opentripplanner.ext.mobilityprofile;

import java.util.Map;
import javax.annotation.Nonnull;

public record MobilityProfileData(
  float lengthInMeters,

  @Nonnull Map<MobilityProfile, Float> costs
) {}
