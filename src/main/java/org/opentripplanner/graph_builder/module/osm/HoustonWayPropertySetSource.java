package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for the Houston, Texas, USA area.
 * The differences compared to the default property set are:
 * 1. In Houston we want to disallow usage of downtown pedestrian tunnel system.
 *
 * @author demory
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */

public class HoustonWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements

    // Disallow any use of underground indoor pedestrian tunnels
    props.setProperties("highway=footway;layer=-1;tunnel=yes;indoor=yes", StreetTraversalPermission.NONE);

    // Read the rest from the default set
    new DefaultWayPropertySetSource().populateProperties(props);
  }

  @Override
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  @Override
  public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
    return new SimpleIntersectionTraversalCostModel(drivingDirection);
  }
}