package org.opentripplanner.ext.mobilityprofile;

import java.util.Map;
import javax.annotation.Nonnull;

public record MobilityProfileData(
  float lengthInMeters,

  long fromNode,

  long toNode,

  @Nonnull Map<MobilityProfile, Float> costs
) {}
