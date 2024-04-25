package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.Iterables;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.mobilityprofile.MobilityProfile;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileData;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileParser;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileRouting;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmProcessingParameters;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OsmModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(OsmModule.class);

  private final Map<Vertex, Double> elevationData = new HashMap<>();

  /**
   * Providers of OSM data.
   */
  private final List<OsmProvider> providers;
  private final Graph graph;
  private final DataImportIssueStore issueStore;
  private final OsmProcessingParameters params;
  private final SafetyValueNormalizer normalizer;
  private final VertexGenerator vertexGenerator;
  private final OsmDatabase osmdb;
  private Map<String, MobilityProfileData> mobilityProfileData;
  private HashSet<String> mappedMobilityProfileEntries;
  private List<OSMWay> osmStreets;
  private final StreetLimitationParameters streetLimitationParameters;

  OsmModule(
    Collection<OsmProvider> providers,
    Graph graph,
    DataImportIssueStore issueStore,
    @Nonnull StreetLimitationParameters streetLimitationParameters,
    OsmProcessingParameters params
  ) {
    this.providers = List.copyOf(providers);
    this.graph = graph;
    this.issueStore = issueStore;
    this.params = params;
    this.osmdb = new OsmDatabase(issueStore);
    this.vertexGenerator = new VertexGenerator(osmdb, graph, params.boardingAreaRefTags());
    this.normalizer = new SafetyValueNormalizer(graph, issueStore);
    this.streetLimitationParameters = Objects.requireNonNull(streetLimitationParameters);
  }

  public static OsmModuleBuilder of(Collection<OsmProvider> providers, Graph graph) {
    return new OsmModuleBuilder(providers, graph);
  }

  public static OsmModuleBuilder of(OsmProvider provider, Graph graph) {
    return of(List.of(provider), graph);
  }

  @Override
  public void buildGraph() {
    for (OsmProvider provider : providers) {
      LOG.info("Gathering OSM from provider: {}", provider);
      LOG.info(
        "Using OSM way configuration from {}.",
        provider.getOsmTagMapper().getClass().getSimpleName()
      );
      provider.readOSM(osmdb);
    }
    osmdb.postLoad();

    LOG.info("Building street graph from OSM");
    build();
    graph.hasStreets = true;
    streetLimitationParameters.initMaxCarSpeed(getMaxCarSpeed());
  }

  @Override
  public void checkInputs() {
    for (OsmProvider provider : providers) {
      provider.checkInputs();
    }
  }

  public Map<Vertex, Double> elevationDataOutput() {
    return elevationData;
  }

  public void setMobilityProfileData(Map<String, MobilityProfileData> mobilityProfileData) {
    this.mobilityProfileData = mobilityProfileData;
  }

  private record StreetEdgePair(StreetEdge main, StreetEdge back) {}

  private void build() {
    var parkingProcessor = new ParkingProcessor(
      graph,
      issueStore,
      vertexGenerator::getVertexForOsmNode
    );

    var parkingLots = new ArrayList<VehicleParking>();

    if (params.staticParkAndRide()) {
      var carParkingNodes = parkingProcessor.buildParkAndRideNodes(
        osmdb.getCarParkingNodes(),
        true
      );
      parkingLots.addAll(carParkingNodes);
    }
    if (params.staticBikeParkAndRide()) {
      var bikeParkingNodes = parkingProcessor.buildParkAndRideNodes(
        osmdb.getBikeParkingNodes(),
        false
      );
      parkingLots.addAll(bikeParkingNodes);
    }

    for (Area area : Iterables.concat(
      osmdb.getWalkableAreas(),
      osmdb.getParkAndRideAreas(),
      osmdb.getBikeParkingAreas()
    )) setWayName(area.parent);

    // figure out which nodes that are actually intersections
    vertexGenerator.initIntersectionNodes();

    mappedMobilityProfileEntries = new HashSet<>();

    buildBasicGraph();
    buildWalkableAreas(!params.areaVisibility());
    validateBarriers();

    if (params.staticParkAndRide()) {
      List<AreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
      var carParkingAreas = parkingProcessor.buildParkAndRideAreas(areaGroups);
      parkingLots.addAll(carParkingAreas);
      LOG.info("Created {} car P+R areas.", carParkingAreas.size());
    }
    if (params.staticBikeParkAndRide()) {
      List<AreaGroup> areaGroups = groupAreas(osmdb.getBikeParkingAreas());
      var bikeParkingAreas = parkingProcessor.buildBikeParkAndRideAreas(areaGroups);
      parkingLots.addAll(bikeParkingAreas);
      LOG.info("Created {} bike P+R areas", bikeParkingAreas.size());
    }

    if (!parkingLots.isEmpty()) {
      graph.getVehicleParkingService().updateVehicleParking(parkingLots, List.of());
    }

    var elevatorProcessor = new ElevatorProcessor(issueStore, osmdb, vertexGenerator);
    elevatorProcessor.buildElevatorEdges(graph);

    TurnRestrictionUnifier.unifyTurnRestrictions(osmdb, issueStore);

    params.edgeNamer().postprocess();

    normalizer.applySafetyFactors();

    listUnusedMobilityCosts();
  }

  /**
   * Lists unused entries from the mobility profile data.
   */
  private void listUnusedMobilityCosts() {
    if (mobilityProfileData != null) {
      List<String> unusedEntries = mobilityProfileData
        .keySet()
        .stream()
        .filter(key -> !mappedMobilityProfileEntries.contains(key))
        .toList();

      if (!unusedEntries.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (var entry : unusedEntries) {
          sb.append(String.format("%n- %s", entry));
        }
        LOG.warn("{} mobility profile entries were not used:{}", unusedEntries.size(), sb);
      }
    }
  }

  /**
   * Returns the length of the geometry in meters.
   */
  private static double getGeometryLengthMeters(Geometry geometry) {
    Coordinate[] coordinates = geometry.getCoordinates();
    double d = 0;
    for (int i = 1; i < coordinates.length; ++i) {
      d += SphericalDistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
    }
    return d;
  }

  private List<AreaGroup> groupAreas(Collection<Area> areas) {
    Map<Area, OSMLevel> areasLevels = new HashMap<>(areas.size());
    for (Area area : areas) {
      areasLevels.put(area, osmdb.getLevelForWay(area.parent));
    }
    return AreaGroup.groupAreas(areasLevels);
  }

  private void buildWalkableAreas(boolean skipVisibility) {
    if (skipVisibility) {
      LOG.info(
        "Skipping visibility graph construction for walkable areas and using just area rings for edges."
      );
    } else {
      LOG.info("Building visibility graphs for walkable areas.");
    }
    List<AreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
    WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
      graph,
      osmdb,
      vertexGenerator,
      params.edgeNamer(),
      normalizer,
      issueStore,
      params.maxAreaNodes(),
      params.platformEntriesLinking(),
      params.boardingAreaRefTags()
    );
    if (skipVisibility) {
      for (AreaGroup group : areaGroups) {
        walkableAreaBuilder.buildWithoutVisibility(group);
      }
    } else {
      ProgressTracker progress = ProgressTracker.track(
        "Build visibility graph for areas",
        50,
        areaGroups.size()
      );
      for (AreaGroup group : areaGroups) {
        walkableAreaBuilder.buildWithVisibility(group);
        //Keep lambda! A method-ref would log incorrect class and line number
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      }
      LOG.info(progress.completeMessage());
    }

    if (skipVisibility) {
      LOG.info("Done building rings for walkable areas.");
    } else {
      LOG.info("Done building visibility graphs for walkable areas.");
    }
  }

  private void buildBasicGraph() {
    /* build the street segment graph from OSM ways */
    long wayCount = osmdb.getWays().size();
    ProgressTracker progress = ProgressTracker.track("Build street graph", 5_000, wayCount);
    LOG.info(progress.startMessage());
    var escalatorProcessor = new EscalatorProcessor(vertexGenerator.intersectionNodes());

    WAY:for (OSMWay way : osmdb.getWays()) {
      WayProperties wayData = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
      setWayName(way);

      var permissions = wayData.getPermission();

      if (!way.isRoutable() || permissions.allowsNothing()) {
        continue;
      }

      // handle duplicate nodes in OSM ways
      // this is a workaround for crappy OSM data quality
      ArrayList<Long> nodes = new ArrayList<>(way.getNodeRefs().size());
      long last = -1;
      double lastLat = -1, lastLon = -1;
      String lastLevel = null;
      for (TLongIterator iter = way.getNodeRefs().iterator(); iter.hasNext();) {
        long nodeId = iter.next();
        OSMNode node = osmdb.getNode(nodeId);
        if (node == null) continue WAY;
        boolean levelsDiffer = false;
        String level = node.getTag("level");
        if (lastLevel == null) {
          if (level != null) {
            levelsDiffer = true;
          }
        } else {
          if (!lastLevel.equals(level)) {
            levelsDiffer = true;
          }
        }
        if (
          nodeId != last && (node.lat != lastLat || node.lon != lastLon || levelsDiffer)
        ) nodes.add(nodeId);
        last = nodeId;
        lastLon = node.lon;
        lastLat = node.lat;
        lastLevel = level;
      }

      IntersectionVertex startEndpoint = null;
      IntersectionVertex endEndpoint = null;

      ArrayList<Coordinate> segmentCoordinates = new ArrayList<>();

      /*
       * Traverse through all the nodes of this edge. For nodes which are not shared with any other edge, do not create endpoints -- just
       * accumulate them for geometry and ele tags. For nodes which are shared, create endpoints and StreetVertex instances. One exception:
       * if the next vertex also appears earlier in the way, we need to split the way, because otherwise we have a way that loops from a
       * vertex to itself, which could cause issues with splitting.
       */
      Long startNode = null;
      // where the current edge should start
      OSMNode osmStartNode = null;

      for (int i = 0; i < nodes.size() - 1; i++) {
        OSMNode segmentStartOSMNode = osmdb.getNode(nodes.get(i));

        if (segmentStartOSMNode == null) {
          continue;
        }

        Long endNode = nodes.get(i + 1);

        if (osmStartNode == null) {
          startNode = nodes.get(i);
          osmStartNode = segmentStartOSMNode;
        }
        // where the current edge might end
        OSMNode osmEndNode = osmdb.getNode(endNode);

        LineString geometry;

        /*
         * We split segments at intersections, self-intersections, nodes with ele tags, and transit stops;
         * the only processing we do on other nodes is to accumulate their geometry
         */
        if (segmentCoordinates.size() == 0) {
          segmentCoordinates.add(osmStartNode.getCoordinate());
        }

        if (
          vertexGenerator.intersectionNodes().containsKey(endNode) ||
          i == nodes.size() - 2 ||
          nodes.subList(0, i).contains(nodes.get(i)) ||
          osmEndNode.hasTag("ele") ||
          osmEndNode.isBoardingLocation() ||
          osmEndNode.isBarrier()
        ) {
          segmentCoordinates.add(osmEndNode.getCoordinate());

          geometry =
            GeometryUtils
              .getGeometryFactory()
              .createLineString(segmentCoordinates.toArray(new Coordinate[0]));
          segmentCoordinates.clear();
        } else {
          segmentCoordinates.add(osmEndNode.getCoordinate());
          continue;
        }

        /* generate endpoints */
        if (startEndpoint == null) { // first iteration on this way
          // make or get a shared vertex for flat intersections,
          // one vertex per level for multilevel nodes like elevators
          startEndpoint = vertexGenerator.getVertexForOsmNode(osmStartNode, way);
          String ele = segmentStartOSMNode.getTag("ele");
          if (ele != null) {
            Double elevation = ElevationUtils.parseEleTag(ele);
            if (elevation != null) {
              elevationData.put(startEndpoint, elevation);
            }
          }
        } else { // subsequent iterations
          startEndpoint = endEndpoint;
        }

        endEndpoint = vertexGenerator.getVertexForOsmNode(osmEndNode, way);
        String ele = osmEndNode.getTag("ele");
        if (ele != null) {
          Double elevation = ElevationUtils.parseEleTag(ele);
          if (elevation != null) {
            elevationData.put(endEndpoint, elevation);
          }
        }
        if (way.isEscalator()) {
          var length = getGeometryLengthMeters(geometry);
          escalatorProcessor.buildEscalatorEdge(way, length);
        } else {
          StreetEdgePair streets = getEdgesForStreet(
            startEndpoint,
            endEndpoint,
            way,
            i,
            permissions,
            geometry
          );

          StreetEdge street = streets.main;
          StreetEdge backStreet = streets.back;
          normalizer.applyWayProperties(street, backStreet, wayData, way);

          applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
          startNode = endNode;
          osmStartNode = osmdb.getNode(startNode);
        }
      }

      //Keep lambda! A method-ref would log incorrect class and line number
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    } // END loop over OSM ways

    LOG.info(progress.completeMessage());
  }

  private void validateBarriers() {
    List<BarrierVertex> vertices = graph.getVerticesOfType(BarrierVertex.class);
    vertices.forEach(bv -> bv.makeBarrierAtEndReachable());
  }

  private void setWayName(OSMWithTags way) {
    if (!way.hasTag("name")) {
      I18NString creativeName = way.getOsmProvider().getWayPropertySet().getCreativeNameForWay(way);
      if (creativeName != null) {
        way.setCreativeName(creativeName);
      }
    }
  }

  private void applyEdgesToTurnRestrictions(
    OSMWay way,
    long startNode,
    long endNode,
    StreetEdge street,
    StreetEdge backStreet
  ) {
    /* Check if there are turn restrictions starting on this segment */
    Collection<TurnRestrictionTag> restrictionTags = osmdb.getFromWayTurnRestrictions(way.getId());

    if (restrictionTags != null) {
      for (TurnRestrictionTag tag : restrictionTags) {
        if (tag.via == startNode) {
          tag.possibleFrom.add(backStreet);
        } else if (tag.via == endNode) {
          tag.possibleFrom.add(street);
        }
      }
    }

    restrictionTags = osmdb.getToWayTurnRestrictions(way.getId());
    if (restrictionTags != null) {
      for (TurnRestrictionTag tag : restrictionTags) {
        if (tag.via == startNode) {
          tag.possibleTo.add(street);
        } else if (tag.via == endNode) {
          tag.possibleTo.add(backStreet);
        }
      }
    }
  }

  /**
   * Handle oneway streets, cycleways, and other per-mode and universal access controls. See
   * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
   * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
   */
  private StreetEdgePair getEdgesForStreet(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    OSMWay way,
    int index,
    StreetTraversalPermission permissions,
    LineString geometry
  ) {
    // No point in returning edges that can't be traversed by anyone.
    if (permissions.allowsNothing()) {
      return new StreetEdgePair(null, null);
    }

    LineString backGeometry = geometry.reverse();
    StreetEdge street = null;
    StreetEdge backStreet = null;
    double length = getGeometryLengthMeters(geometry);

    var permissionPair = way.splitPermissions(permissions);
    var permissionsFront = permissionPair.main();
    var permissionsBack = permissionPair.back();

    if (permissionsFront.allowsAnything()) {
      street =
        getEdgeForStreet(
          startEndpoint,
          endEndpoint,
          way,
          index,
          length,
          permissionsFront,
          geometry,
          false
        );
    }
    if (permissionsBack.allowsAnything()) {
      backStreet =
        getEdgeForStreet(
          endEndpoint,
          startEndpoint,
          way,
          index,
          length,
          permissionsBack,
          backGeometry,
          true
        );
    }
    if (street != null && backStreet != null) {
      backStreet.shareData(street);
    }
    return new StreetEdgePair(street, backStreet);
  }

  private StreetEdge getEdgeForStreet(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    OSMWay way,
    int index,
    double length,
    StreetTraversalPermission permissions,
    LineString geometry,
    boolean back
  ) {
    long wayId = way.getId();
    String label = "way " + wayId + " from " + index;
    label = label.intern();
    I18NString name = params.edgeNamer().getNameForWay(way, label);
    float carSpeed = way.getOsmProvider().getOsmTagMapper().getCarSpeedForWay(way, back);

    StreetTraversalPermission perms = mobilityProfileData != null
      ? MobilityProfileRouting.adjustPedestrianPermissions(way, permissions)
      : permissions;

    StreetEdgeBuilder<?> seb = new StreetEdgeBuilder<>()
      .withFromVertex(startEndpoint)
      .withToVertex(endEndpoint)
      .withGeometry(geometry)
      .withName(name)
      .withMeterLength(length)
      .withPermission(perms)
      .withBack(back)
      .withCarSpeed(carSpeed)
      .withLink(way.isLink())
      .withRoundabout(way.isRoundabout())
      .withSlopeOverride(way.getOsmProvider().getWayPropertySet().getSlopeOverride(way))
      .withStairs(way.isSteps())
      .withWheelchairAccessible(way.isWheelchairAccessible());

    boolean hasBogusName = !way.hasTag("name") && !way.hasTag("ref");

    // If this is a street crossing (denoted with the tag "footway:crossing"),
    // add a crossing indication in the edge name.
    String editedName = name.toString();
    if (way.isMarkedCrossing()) {
      // Scan the nodes of this way to find the intersecting street.
      var otherWayOpt = getIntersectingStreet(way);
      if (otherWayOpt.isPresent()) {
        OSMWay otherWay = otherWayOpt.get();
        if (otherWay.hasTag("name")) {
          editedName = String.format("crossing over %s", otherWay.getTag("name"));
        } else if (otherWay.isServiceRoad()) {
          editedName = "crossing over service road";
        } else if (otherWay.isOneWayForwardDriving()) {
          editedName = "crossing over turn lane";
        } else {
          editedName = String.format("crossing %s", wayId);
        }
      }

      seb.withName(editedName);
      hasBogusName = false;
    } else if ("sidewalk".equals(editedName) || "path".equals(editedName)) {
      editedName = String.format("%s %s", editedName, wayId);
    }

    // Lookup costs by mobility profile, if any were defined.
    // Note that edges are bidirectional, so we check that mobility data exist in both directions.
    if (mobilityProfileData != null) {
      String startId = startEndpoint.getLabel().toString();
      String endId = endEndpoint.getLabel().toString();

      try {
        long startShortId = Long.parseLong(startId.replace("osm:node:", ""), 10);
        long endShortId = Long.parseLong(endId.replace("osm:node:", ""), 10);

        // For testing, indicate the OSM node ids (remove prefixes).
        String nameWithNodeIds = String.format(
          "%s (%s, %s→%s)",
          editedName,
          wayId,
          startShortId,
          endShortId
        );

        seb.withName(nameWithNodeIds);

        String wayIdStr = Long.toString(wayId, 10);
        TLongList nodeRefs = way.getNodeRefs();
        int startIndex = nodeRefs.indexOf(startShortId);
        int endIndex = nodeRefs.indexOf(endShortId);
        boolean isReverse = endIndex < startIndex;

        // Use the start and end nodes of the OSM way per the OSM data to lookup the mobility costs.
        long wayFromId = nodeRefs.get(0);
        long wayToId = nodeRefs.get(nodeRefs.size() - 1);
        String key = isReverse
          ? MobilityProfileParser.getKey(wayIdStr, wayToId, wayFromId)
          : MobilityProfileParser.getKey(wayIdStr, wayFromId, wayToId);

        var edgeMobilityCostMap = mobilityProfileData.get(key);
        if (edgeMobilityCostMap != null) {
          // Check whether the nodes for this way match the nodes from mobility profile data.
          if (
            startShortId == edgeMobilityCostMap.fromNode() &&
            endShortId == edgeMobilityCostMap.toNode() ||
            startShortId == edgeMobilityCostMap.toNode() &&
            endShortId == edgeMobilityCostMap.fromNode()
          ) {
            // If the from/to nodes match, then assign the cost directly
            seb.withProfileCosts(edgeMobilityCostMap.costs());

            // Append an indication that this edge uses a full profile cost.
            nameWithNodeIds = String.format("%s ☑", nameWithNodeIds);
            // System.out.printf("Way (full length): %s size %d%n", nameWithNodeIds, edgeMobilityCostMap.costs().size());
            System.out.printf(
              "%s %f%n",
              nameWithNodeIds,
              edgeMobilityCostMap.costs().get(MobilityProfile.WCHAIRE)
            );
          } else {
            // Otherwise, pro-rate the cost to the length of the edge.
            float ratio = (float) (length / edgeMobilityCostMap.lengthInMeters());

            Map<MobilityProfile, Float> proRatedProfileCosts = MobilityProfileRouting.getProRatedProfileCosts(
              edgeMobilityCostMap.costs(),
              ratio
            );
            seb.withProfileCosts(proRatedProfileCosts);

            // Append an indication that this edge uses a partial profile cost.
            nameWithNodeIds = String.format("%s r%4.3f l%4.3f", nameWithNodeIds, ratio, length);
            // System.out.printf("Way (partial): %s size %d%n", nameWithNodeIds, proRatedProfileCosts.size());
            System.out.printf(
              "%s %f%n",
              nameWithNodeIds,
              proRatedProfileCosts.get(MobilityProfile.WCHAIRE)
            );
          }

          seb.withName(nameWithNodeIds);

          // Keep tab of node pairs for which mobility profile costs have been mapped.
          mappedMobilityProfileEntries.add(key);
        }
      } catch (NumberFormatException nfe) {
        // Don't do anything related to mobility profiles if node ids are non-numerical.
        LOG.info(
          "Not applying mobility costs for link {}:{}→{}",
          wayId,
          startEndpoint.getLabel(),
          endEndpoint.getLabel()
        );
      }
    }

    seb.withBogusName(hasBogusName);

    StreetEdge street = seb.buildAndConnect();
    params.edgeNamer().recordEdge(way, street);

    return street;
  }

  private Optional<OSMWay> getIntersectingStreet(OSMWay way) {
    if (osmStreets == null) {
      osmStreets =
        osmdb
          .getWays()
          .stream()
          .filter(w -> !w.isFootway())
          // Keep named streets, service roads, and slip/turn lanes.
          .filter(w -> w.hasTag("name") || w.isServiceRoad() || w.isOneWayForwardDriving())
          .toList();
    }

    TLongList nodeRefs = way.getNodeRefs();
    if (nodeRefs.size() >= 3) {
      // Exclude the first and last node which are on the sidewalk.
      long[] nodeRefsArray = nodeRefs.toArray(1, nodeRefs.size() - 2);
      return osmStreets
        .stream()
        .filter(w -> Arrays.stream(nodeRefsArray).anyMatch(nid -> w.getNodeRefs().contains(nid)))
        .findFirst();
    }
    return Optional.empty();
  }

  private float getMaxCarSpeed() {
    float maxSpeed = 0f;
    for (OsmProvider provider : providers) {
      var carSpeed = provider.getOsmTagMapper().getMaxUsedCarSpeed(provider.getWayPropertySet());
      if (carSpeed > maxSpeed) {
        maxSpeed = carSpeed;
      }
    }
    return maxSpeed;
  }
}
