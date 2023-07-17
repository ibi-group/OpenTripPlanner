package org.opentripplanner.standalone.config.routerequest;

import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class TimeAndCostPenaltyMapper {

  public static TimeAndCostPenalty map(NodeAdapter c) {
    return TimeAndCostPenalty.of(
      c
        .of("timePenalty")
        .summary("Penalty added to the time of a leg.")
        .description(TimePenalty.DOC)
        .since(OtpVersion.V2_4)
        .asString(TimeAndCostPenalty.ZERO.timePenalty().toString()),
      c
        .of("costFactor")
        .summary("A factor multiplied with the time-penalty to get the cost-penalty.")
        .since(OtpVersion.V2_4)
        .asDouble(TimeAndCostPenalty.ZERO.costFactor())
    );
  }
}
