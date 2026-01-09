package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;

public record OnlyOneIntersectionNodeInElevatorWay(OsmWay way) implements DataImportIssue {
  private static final String FMT =
    "Elevator way %s has only one intersection node. " +
    "This makes the elevator unusable. " +
    "Please check whether the way is correctly modeled.";

  private static final String HTMLFMT =
    "<a href='%s'>Elevator way %s</a> has only one intersection node. " +
    "This makes the elevator unusable. " +
    "Please check whether the way is correctly modeled.";

  @Override
  public String getMessage() {
    return String.format(FMT, way.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, way.url(), way.getId());
  }
}
