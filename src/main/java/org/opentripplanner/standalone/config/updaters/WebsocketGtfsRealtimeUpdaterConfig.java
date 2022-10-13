package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.WebsocketGtfsRealtimeUpdaterParameters;

public class WebsocketGtfsRealtimeUpdaterConfig {

  public static WebsocketGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WebsocketGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(null),
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(null),
      c.of("reconnectPeriodSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c
        .of("backwardsDelayPropagationType")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
