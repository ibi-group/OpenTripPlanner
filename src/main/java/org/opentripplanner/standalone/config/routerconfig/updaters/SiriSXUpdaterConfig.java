package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriSXUpdaterConfig {

  public static SiriSXUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXUpdaterParameters(
      configRef,
      c.of("feedId").since(V2_0).summary("The ID of the feed to apply the updates to.").asString(),
      c.of("url").since(V2_0).summary("The URL to send the HTTP requests to.").asString(),
      c.of("requestorRef").since(V2_0).summary("The requester reference.").asString(null),
      c
        .of("frequencySec")
        .since(V2_0)
        .summary("How often the updates should be retrieved.")
        .asInt(60),
      c.of("earlyStartSec").since(V2_0).summary("TODO").asInt(-1),
      c.of("timeoutSec").since(V2_0).summary("The HTTP timeout to download the updates.").asInt(15),
      c
        .of("blockReadinessUntilInitialized")
        .since(V2_0)
        .summary(
          "Whether catching up with the updates should block the readiness check from returning a 'ready' result."
        )
        .asBoolean(false),
      HttpHeadersConfig.headers(c, V2_3)
    );
  }
}
