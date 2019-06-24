package org.opentripplanner.updater.vehicle_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.graph_builder.linking.StreetSplitter;
import org.opentripplanner.routing.edgetype.RentAVehicleOffEdge;
import org.opentripplanner.routing.edgetype.RentAVehicleOnEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalRegion;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.opentripplanner.graph_builder.linking.SimpleStreetSplitter.DESTRUCTIVE_SPLIT;

public class VehicleRentalUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalUpdater.class);

    private static DecimalFormat format = new DecimalFormat("##.000000");

    private Graph graph;

    private StreetSplitter splitter;

    private String network;

    private VehicleRentalStationService service;

    private VehicleRentalDataSource source;

    private GraphUpdaterManager updaterManager;

    Map<VehicleRentalStation, VehicleRentalStationVertex> verticesByStation = new HashMap<>();

    @Override
    protected void runPolling() {
        LOG.debug("Updating vehicle rental stations and regions from " + source);
        List<VehicleRentalRegion> regions = new ArrayList<>();
        List<VehicleRentalStation> stations = new ArrayList<>();
        source.update();
        if (source.stationsUpdated()) {
            stations = source.getStations();
        } else {
            LOG.debug("No station updates");
        }

        if (source.regionsUpdated()) {
            regions = source.getRegions();
        } else {
            LOG.debug("No region updates");
        }

        // Create graph writer runnable to apply these stations and regions to the graph
        updaterManager.execute(new VehicleRentalGraphWriterRunnable(stations, regions));
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        network = config.path("network").asText();
        VehicleRentalDataSource source = null;
        String sourceType = config.path("sourceType").asText();
        if (sourceType != null) {
            if (sourceType.equals("gbfs")) {
                source = new GenericGbfsService();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown vehicle rental source type: " + network);
        } else if (source instanceof JsonConfigurable) {
            ((JsonConfigurable) source).configure(graph, config);
        }

        // Configure updater
        LOG.info("Setting up vehicle rental updater.");
        this.graph = graph;
        this.source = source;
        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating vehicle rental updater running once only (non-polling): {}", source);
        } else {
            LOG.info("Creating vehicle rental updater running every {} seconds : {}", pollingPeriodSeconds, source);
        }
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        splitter = graph.streetIndex.getStreetSplitter();

        service = graph.getService(VehicleRentalStationService.class, true);
    }

    @Override
    public void teardown() {}

    private class VehicleRentalGraphWriterRunnable implements GraphWriterRunnable {
        private List<VehicleRentalStation> stations;
        private List<VehicleRentalRegion> regions;
        private GeometryFactory geometryFactory = new GeometryFactory();


        public VehicleRentalGraphWriterRunnable(List<VehicleRentalStation> stations, List<VehicleRentalRegion> regions) {
            this.stations = stations;
            this.regions = regions;
        }

        @Override
        public void run(Graph graph) {
            if (!this.stations.isEmpty()) {
                applyStations(graph);
            }
            if (!this.regions.isEmpty()) {
                applyRegions(graph);
            }
        }

        private void applyStations(Graph graph) {
            // Apply stations to graph
            Set<VehicleRentalStation> stationSet = new HashSet<>();
            Set<String> defaultNetworks = new HashSet<>(Arrays.asList(network));
            LOG.info("Updating {} vehicle rental stations for network {}.", stations.size(), network);
            /* add any new stations and update vehicle counts for existing stations */
            for (VehicleRentalStation station : stations) {
                service.addVehicleRentalStation(station);
                stationSet.add(station);
                VehicleRentalStationVertex vertex = verticesByStation.get(station);
                if (vertex == null) {
                    makeVertex(graph, station);
                } else if (station.x != vertex.getX() || station.y != vertex.getY()) {
                    LOG.info("{} has changed, re-graphing", station);

                    // First remove the old one.
                    if (graph.containsVertex(vertex)) {
                        graph.removeVertexAndEdges(vertex);
                    }

                    makeVertex(graph, station);
                } else {
                    vertex.setVehiclesAvailable(station.vehiclesAvailable);
                    vertex.setSpacesAvailable(station.spacesAvailable);
                }
            }
            // Remove existing stations that were not present in the update
            List<VehicleRentalStation> toRemove = new ArrayList<>();
            for (Entry<VehicleRentalStation, VehicleRentalStationVertex> entry : verticesByStation.entrySet()) {
                VehicleRentalStation station = entry.getKey();
                if (stationSet.contains(station))
                    continue;
                VehicleRentalStationVertex vertex = entry.getValue();
                if (graph.containsVertex(vertex)) {
                    graph.removeVertexAndEdges(vertex);
                }
                toRemove.add(station);
                service.removeVehicleRentalStation(station);
                // TODO: need to unsplit any streets that were split
            }
            for (VehicleRentalStation station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByStation.remove(station);
            }
        }

        private void makeVertex(Graph graph, VehicleRentalStation station) {
            VehicleRentalStationVertex vertex = new VehicleRentalStationVertex(graph, station);
            if (!splitter.linkToClosestWalkableEdge(vertex, DESTRUCTIVE_SPLIT)) {
                // the toString includes the text "Vehicle rental station"
                LOG.warn("{} not near any streets; it will not be usable.", station);
            }
            verticesByStation.put(station, vertex);
            if (station.allowPickup)
                new RentAVehicleOnEdge(vertex, station);
            if (station.allowDropoff)
                new RentAVehicleOffEdge(vertex, station);
        }

        public void applyRegions(Graph graph) {
            // Adding vehicle rental regions to all edges of the network.
            Map<Coordinate, Set<String>> coordToNetworksMap = new HashMap<>();
            Collection<StreetEdge> edges = graph.getStreetEdges();
            for (VehicleRentalRegion region : regions) {
                LOG.info("Applying vehicle rental region for: {}", region.network);
                service.addVehicleRentalRegion(region);
                Set<Coordinate> coordinates = intersectWithGraph(edges, region);

                coordinates.forEach(c -> coordToNetworksMap.putIfAbsent(c, new HashSet<>()));
                coordinates.forEach(c -> coordToNetworksMap.get(c).add(region.network));

            }
            LOG.info("Adding dropoffs to graph");
            addDropOffsToGraph(coordToNetworksMap);
            LOG.info("Added in total {} border dropoffs.", coordToNetworksMap.size());
        }

        /**
         * Labels edges that are inside the region with the vehicle network name. For edges that are partially
         * inside the region, computes the intersection points and returns them.
         * Skips edges that are outside of the region.
         * @param edges
         * @param region The intersection locations
         */
        private Set<Coordinate> intersectWithGraph(Collection<StreetEdge> edges, VehicleRentalRegion region) {
            Set<Coordinate> coordinates = new HashSet<>();

            // use a prepared geometry to dramatically speed up "covers" operations
            PreparedGeometry preparedRegionGeometry = PreparedGeometryFactory.prepare(region.geometry);

            // Iterate through StreetEdges
            for (StreetEdge edge : edges) {
                Point[] edgePoints = getEdgeCoord(edge);

                // does this check if all of the edge is covered? What about a really windy road?
                boolean coversFrom = preparedRegionGeometry.covers(edgePoints[0]);
                boolean coversTo = preparedRegionGeometry.covers(edgePoints[1]);

                if (coversFrom && coversTo) {
                    // all of edge is within region
                    edge.addVehicleNetwork(region.network);
                } else if (coversFrom || coversTo) {
                    // part of edge is within region
                    coordinates.addAll(intersect(edgePoints, region));
                }
            }
            return coordinates;
        }

        /**
         * Finds the intersection points of the edge and the region.
         *
         * @param edgePoints an array of two points representing an edge
         * @param region the area inside which vehicle rentals are allowed to be dropped off
         * @return intersection coordinates
         */
        private Set<Coordinate> intersect(Point[] edgePoints, VehicleRentalRegion region) {
            // Intersect the edge with the region's geometry.
            Point from = edgePoints[0];
            Point to = edgePoints[1];
            LineString lineString = geometryFactory.createLineString(new Coordinate[]{from.getCoordinate(), to.getCoordinate()});
            Geometry intersectionLineStrings = region.geometry.intersection(lineString);
            // intersection results in one or more linestrings representing the parts of the edge that overlaps
            // the polygon.
            Set<Coordinate> intersectionPoints = new HashSet<>();
            for (int i = 0; i < intersectionLineStrings.getNumGeometries(); i++) {
                Geometry intersectionLineString = intersectionLineStrings.getGeometryN(i);
                for (Coordinate coordinate : intersectionLineString.getCoordinates()) {
                    if (coordinate.equals(equals(from.getCoordinate())) ||
                        coordinate.equals(equals(to.getCoordinate()))){
                        // Skip the 'from' and 'to' nodes of the edge and only return the intersection points.
                        // The reason is that we will create vehicle drop off stations for those
                        // intersection points, and not for 'from' and 'to' nodes.
                        continue;
                    }
                    intersectionPoints.add(roundLatLng(coordinate));
                }
            }
            return intersectionPoints;
        }

        private Coordinate roundLatLng(Coordinate coordinate) {
            return new Coordinate(Double.parseDouble(format.format(coordinate.x)), Double.parseDouble(format.format(coordinate.y)), 0);
        }

        /**
         * Adds a vehicle dropoff station at each given (location, networks) pairs.
         */
        private void addDropOffsToGraph(Map<Coordinate, Set<String>> coordToNetworksMap) {
            coordToNetworksMap.forEach((coord, networks) -> {
                VehicleRentalStation station = makeDropOffStation(coord, networks);
                VehicleRentalStationVertex vertex = new VehicleRentalStationVertex(graph, station);
                if (!splitter.linkToClosestWalkableEdge(vertex, DESTRUCTIVE_SPLIT)) {
                    LOG.warn("Ignoring {} since it's not near any streets; it will not be usable.", station);
                    return;
                }
                service.addVehicleRentalStation(station);
                new RentAVehicleOffEdge(vertex, station);
            });
        }

        /**
         * @param edge an Edge
         * @return an array of size 2 of Point, with the first and the second items representing the edge's
         *         `from` and `to` locations respectively.
         */
        private Point[] getEdgeCoord(final Edge edge) {
            Point fromPoint = geometryFactory.createPoint(new Coordinate(edge.getFromVertex().getX(), edge.getFromVertex().getY(), 0));
            Point toPoint = geometryFactory.createPoint(new Coordinate(edge.getToVertex().getX(), edge.getToVertex().getY(), 0));
            return new Point[]{fromPoint, toPoint};
        }

        private VehicleRentalStation makeDropOffStation(Coordinate coord, Set<String> networks) {
            VehicleRentalStation station = new VehicleRentalStation();
            String networkNames = (networks == null || networks.size() == 0) ? "undefined network" : networks.toString();
            String id = String.format("border_dropoff_%3.6f_%3.6f_%s", coord.x, coord.y, networkNames);

            station.allowDropoff = true;
            station.allowPickup = false;
            station.vehiclesAvailable = 0;
            station.id = id;
            station.isBorderDropoff = true;
            station.name = new NonLocalizedString(id);
            station.networks = networks;
            station.spacesAvailable = Integer.MAX_VALUE;
            station.x = coord.x;
            station.y = coord.y;
            return station;
        }
    }
}