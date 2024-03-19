package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;

/**
 * An interface for transit entities that can be filtered by {@link org.opentripplanner.routing.api.request.request.filter.TransitFilter}
 */
public interface TransitFilterable {
  /**
   * The route of the entity.
   */
  Route getRoute();

  /**
   * The mode of the entity.
   */
  TransitMode getMode();

  /**
   * The NeTex submode of the entity.
   */
  SubMode getNetexSubmode();

  /**
   * Does the entity contain multiple modes.
   */
  boolean getContainsMultipleModes();
}
