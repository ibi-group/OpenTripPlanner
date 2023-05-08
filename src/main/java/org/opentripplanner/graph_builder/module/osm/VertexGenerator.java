package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.Iterables;
import gnu.trove.list.TLongList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.ExitVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;

/**
 * Tracks the generation of vertices and returns an existing instance if a vertex is encountered
 * more than once.
 */
class VertexGenerator {

  private static final String nodeLabelFormat = "osm:node:%d";
  private static final String levelnodeLabelFormat = nodeLabelFormat + ":level:%s";

  private final Map<Long, IntersectionVertex> intersectionNodes = new HashMap<>();

  private final HashMap<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes = new HashMap<>();
  private final OsmDatabase osmdb;
  private final Graph graph;
  private final Set<String> boardingAreaRefTags;

  public VertexGenerator(OsmDatabase osmdb, Graph graph, Set<String> boardingAreaRefTags) {
    this.osmdb = osmdb;
    this.graph = graph;
    this.boardingAreaRefTags = boardingAreaRefTags;
  }

  /**
   * Make or get a shared vertex for flat intersections, or one vertex per level for multilevel
   * nodes like elevators. When there is an elevator or other Z-dimension discontinuity, a single
   * node can appear in several ways at different levels.
   *
   * @param node The node to fetch a label for.
   * @param way  The way it is connected to (for fetching level information).
   * @return vertex The graph vertex. This is not always an OSM vertex; it can also be a
   * {@link OsmBoardingLocationVertex}
   */
  IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
    // If the node should be decomposed to multiple levels,
    // use the numeric level because it is unique, the human level may not be (although
    // it will likely lead to some head-scratching if it is not).
    IntersectionVertex iv = null;
    if (node.isMultiLevel()) {
      // make a separate node for every level
      return recordLevel(node, way);
    }
    // single-level case
    long nid = node.getId();
    iv = intersectionNodes.get(nid);
    if (iv == null) {
      Coordinate coordinate = node.getCoordinate();
      String label = String.format(nodeLabelFormat, node.getId());
      String highway = node.getTag("highway");
      if ("motorway_junction".equals(highway)) {
        String ref = node.getTag("ref");
        if (ref != null) {
          ExitVertex ev = new ExitVertex(graph, label, coordinate.x, coordinate.y, nid);
          ev.setExitName(ref);
          iv = ev;
        }
      }

      /* If the OSM node represents a transit stop and has a ref=(stop_code) tag, make a special vertex for it. */
      if (node.isBoardingLocation()) {
        var refs = node.getMultiTagValues(boardingAreaRefTags);
        if (!refs.isEmpty()) {
          String name = node.getTag("name");
          iv =
            new OsmBoardingLocationVertex(
              graph,
              label,
              coordinate.x,
              coordinate.y,
              NonLocalizedString.ofNullable(name),
              refs
            );
        }
      }

      if (node.isBarrier()) {
        BarrierVertex bv = new BarrierVertex(graph, label, coordinate.x, coordinate.y, nid);
        bv.setBarrierPermissions(
          OsmFilter.getPermissionsForEntity(node, BarrierVertex.defaultBarrierPermissions)
        );
        iv = bv;
      }

      if (iv == null) {
        iv =
          new OsmVertex(
            graph,
            label,
            coordinate.x,
            coordinate.y,
            node.getId(),
            new NonLocalizedString(label),
            node.hasHighwayTrafficLight(),
            node.hasCrossingTrafficLight()
          );
      }

      intersectionNodes.put(nid, iv);
    }

    return iv;
  }

  /**
   * Tracks OSM nodes which are decomposed into multiple graph vertices because they are
   * elevators. They can then be iterated over to build {@link ElevatorEdge} between them.
   */
  Map<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes() {
    return multiLevelNodes;
  }

  void initIntersectionNodes() {
    Set<Long> possibleIntersectionNodes = new HashSet<>();
    for (OSMWay way : osmdb.getWays()) {
      TLongList nodes = way.getNodeRefs();
      nodes.forEach(node -> {
        if (possibleIntersectionNodes.contains(node)) {
          intersectionNodes.put(node, null);
        } else {
          possibleIntersectionNodes.add(node);
        }
        return true;
      });
    }
    // Intersect ways at area boundaries if needed.
    for (Area area : Iterables.concat(
      osmdb.getWalkableAreas(),
      osmdb.getParkAndRideAreas(),
      osmdb.getBikeParkingAreas()
    )) {
      for (Ring outerRing : area.outermostRings) {
        intersectAreaRingNodes(possibleIntersectionNodes, outerRing);
      }
    }
  }

  /**
   * Track OSM nodes that will become graph vertices because they appear in multiple OSM ways
   */
  Map<Long, IntersectionVertex> intersectionNodes() {
    return intersectionNodes;
  }

  /**
   * Record the level of the way for this node, e.g. if the way is at level 5, mark that this node
   * is active at level 5.
   *
   * @param way  the way that has the level
   * @param node the node to record for
   * @author mattwigway
   */
  private OsmVertex recordLevel(OSMNode node, OSMWithTags way) {
    OSMLevel level = osmdb.getLevelForWay(way);
    Map<OSMLevel, OsmVertex> vertices;
    long nodeId = node.getId();
    if (multiLevelNodes.containsKey(nodeId)) {
      vertices = multiLevelNodes.get(nodeId);
    } else {
      vertices = new HashMap<>();
      multiLevelNodes.put(nodeId, vertices);
    }
    if (!vertices.containsKey(level)) {
      Coordinate coordinate = node.getCoordinate();
      String label = String.format(levelnodeLabelFormat, node.getId(), level.shortName);
      OsmVertex vertex = new OsmVertex(
        graph,
        label,
        coordinate.x,
        coordinate.y,
        node.getId(),
        new NonLocalizedString(label),
        false,
        false
      );
      vertices.put(level, vertex);

      return vertex;
    }
    return vertices.get(level);
  }

  private void intersectAreaRingNodes(Set<Long> possibleIntersectionNodes, Ring outerRing) {
    for (OSMNode node : outerRing.nodes) {
      long nodeId = node.getId();
      if (possibleIntersectionNodes.contains(nodeId)) {
        intersectionNodes.put(nodeId, null);
      } else {
        possibleIntersectionNodes.add(nodeId);
      }
    }

    outerRing.getHoles().forEach(hole -> intersectAreaRingNodes(possibleIntersectionNodes, hole));
  }
}
