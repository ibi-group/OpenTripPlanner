package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;

public interface Filterable {
  Route getRoute();

  TransitMode getMode();

  SubMode getNetexSubmode();

  boolean getContainsMultipleModes();
}
