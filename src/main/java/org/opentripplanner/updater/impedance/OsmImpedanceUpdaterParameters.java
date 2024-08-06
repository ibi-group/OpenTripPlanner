package org.opentripplanner.updater.impedance;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record OsmImpedanceUpdaterParameters(
  String configRef,
  String url,
  Duration frequency,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters {}
