package org.opentripplanner.osm.model;

import org.opentripplanner.street.geometry.WgsCoordinate;

public class NodeBuilder {

  private final OsmNode node;

  private NodeBuilder(OsmNode node) {
    this.node = node;
  }

  public static OsmNode node(long id, WgsCoordinate wgsCoordinate) {
    return of(id, wgsCoordinate).build();
  }

  public static NodeBuilder of() {
    return new NodeBuilder(new OsmNode());
  }

  public static NodeBuilder of(long id, WgsCoordinate wgsCoordinate) {
    var builder = new NodeBuilder(new OsmNode(wgsCoordinate.latitude(), wgsCoordinate.longitude()));
    builder.node.setId(id);
    return builder;
  }

  public NodeBuilder withTag(String key, String value) {
    node.addTag(key, value);
    return this;
  }

  public OsmNode build() {
    return node;
  }
}
