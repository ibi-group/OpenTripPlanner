package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.sources.VehicleRentalSourceFactory;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public class VehicleRentalUpdaterConfig {

  public static VehicleRentalUpdaterParameters create(String configRef, NodeAdapter c) {
    var sourceType = c.of("sourceType").since(NA).summary("TODO").asEnum(DataSourceType.class);
    return new VehicleRentalUpdaterParameters(
      configRef + "." + sourceType,
      c.of("frequencySec").since(NA).summary("TODO").asInt(60),
      VehicleRentalSourceFactory.create(sourceType, c)
    );
  }
}
