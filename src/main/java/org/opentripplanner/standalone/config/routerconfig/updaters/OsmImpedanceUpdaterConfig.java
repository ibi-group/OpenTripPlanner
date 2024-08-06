package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.impedance.OsmImpedanceUpdaterParameters;

public class OsmImpedanceUpdaterConfig {
  public static OsmImpedanceUpdaterParameters create(String configRef, NodeAdapter c) {
    return new OsmImpedanceUpdaterParameters(
      configRef,
      c.of("url").since(V2_5).summary("URL to fetch the GTFS-RT feed from.").asString(),
      c
        .of("frequency")
        .since(V2_5)
        .summary("How often the URL should be fetched.")
        .asDuration(Duration.ofSeconds(30)),
      HttpHeadersConfig.headers(c, V2_5)
    );
  }
}
