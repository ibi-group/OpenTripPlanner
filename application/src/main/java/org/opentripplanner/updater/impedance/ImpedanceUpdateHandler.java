package org.opentripplanner.updater.impedance;

import java.util.Map;
import org.opentripplanner.ext.mobilityprofile.MobilityProfile;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileData;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileRouting;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes updated impedances to the graph.
 */
public class ImpedanceUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ImpedanceUpdateHandler.class);

  public void update(Graph graph, Map<String, MobilityProfileData> impedances) {
    long start = System.currentTimeMillis();
    long count = 0;

    // Some basic G-MAP stats:
    // - total edges: ~902k
    // - walkable edges: ~92k
    // - First-time impedance load: ~29k entries

    for (StreetEdge se : graph.getStreetEdges()) {
      var impedance = impedances.get(se.profileKey);
      if (impedance != null) {
        String symbol = "★";
        Map<MobilityProfile, Float> proRatedCosts = impedance.costs();

        // Create pro-rated impedances for split edges with intermediate OsmVertex or SplitterVertex.
        long fromNodeId = 0;
        if (se.getFromVertex() instanceof OsmVertex osmFrom) fromNodeId = osmFrom.nodeId;
        long toNodeId = 0;
        if (se.getToVertex() instanceof OsmVertex osmTo) toNodeId = osmTo.nodeId;

        if (fromNodeId != impedance.fromNode() || toNodeId != impedance.toNode()) {
          double ratio = se.getDistanceMeters() / impedance.lengthInMeters();
          proRatedCosts =
            MobilityProfileRouting.getProRatedProfileCosts(impedance.costs(), (float) ratio);
          symbol = "☆";
        }

        // Update profile costs for this StreetEdge object if an impedance entry was found.
        se.profileCost = proRatedCosts;
        count++;

        // Amend the name with an indication that impedances were applied
        se.setName(I18NString.of(String.format("%s%s", se.getName(), symbol)));
      }
    }

    LOG.info(
      "{} new impedance entries imported into graph in {} seconds.",
      count,
      (System.currentTimeMillis() - start) / 1000
    );
  }
}
