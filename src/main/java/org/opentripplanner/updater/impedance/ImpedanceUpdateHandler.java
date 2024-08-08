package org.opentripplanner.updater.impedance;

import java.util.Collection;
import java.util.Map;
import org.opentripplanner.ext.mobilityprofile.MobilityProfile;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileData;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileRouting;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.search.TraverseMode;
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

    // This filter is fast and cuts the size of the searchable set tenfold.
    Collection<StreetEdge> walkableEdges = graph
      .getStreetEdges()
      .stream()
      .filter(se -> se.getPermission().allows(TraverseMode.WALK))
      .toList();

    for (StreetEdge se : walkableEdges) {
      if (
        se.getFromVertex() instanceof OsmVertex osmFrom &&
        se.getToVertex() instanceof OsmVertex osmTo
      ) {
        var impedance = impedances.get(se.profileKey);
        if (impedance != null) {
          String symbol = "★";
          Map<MobilityProfile, Float> proRatedCosts = impedance.costs();

          // Create pro-rated impedance data for split edges.
          if (osmFrom.nodeId != impedance.fromNode() || osmTo.nodeId != impedance.toNode()) {
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
    }

    LOG.info(
      "{} new impedance entries imported into graph in {} seconds.",
      count,
      (System.currentTimeMillis() - start) / 1000
    );
  }
}
