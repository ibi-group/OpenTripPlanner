package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class PlatformLinkerTest {

  /**
   * Test linking from stairs endpoint to nodes in the ring defining the platform area. OSM test
   * data is from Skøyen station, Norway
   */
  @Test
  public void testLinkEntriesToPlatforms() {
    String stairsEndpointLabel = "osm:node:1028861028";

    var deduplicator = new Deduplicator();
    var gg = new Graph(deduplicator);

    File file = new File(
      URLDecoder.decode(
        FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );

    OsmProvider provider = new OsmProvider(file, false);

    OsmModule osmModule = OsmModule.of(provider, gg).withPlatformEntriesLinking(true).build();

    osmModule.buildGraph();

    Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

    // verify outgoing links
    assertTrue(stairsEndpoint.getOutgoing().stream().anyMatch(AreaEdge.class::isInstance));

    // verify incoming links
    assertTrue(stairsEndpoint.getIncoming().stream().anyMatch(AreaEdge.class::isInstance));
  }
}
