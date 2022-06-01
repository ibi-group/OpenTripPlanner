package org.opentripplanner.api.mapping;

import org.opentripplanner.model.WheelchairAccessibility;

public class WheelchairAccessibilityMapper {

  public static Integer mapToApi(WheelchairAccessibility domain) {
    return domain == null ? WheelchairAccessibility.NO_INFORMATION.gtfsCode : domain.gtfsCode;
  }
}
