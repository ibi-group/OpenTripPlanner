package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * This represents the connection between a street vertex and a bike rental station vertex.
 * 
 */
public class StreetBikeRentalLink extends StreetRentalLink {

    private static final long serialVersionUID = 1L;

    public StreetBikeRentalLink(StreetVertex fromv, BikeRentalStationVertex tov) {
        super(fromv, tov);
    }

    public StreetBikeRentalLink(BikeRentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
    }

    public State traverse(State s0) {
        // Do not even consider bike rental vertices unless bike rental is enabled.
        if ( ! s0.getOptions().allowBikeRental) {
            return null;
        }
        // Disallow traversing two StreetBikeRentalLinks in a row.
        // This prevents the router from using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetBikeRentalLink) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        //assume bike rental stations are more-or-less on-street
        s1.incrementTimeInSeconds(1);
        s1.incrementWeight(1);
        s1.setBackMode(s0.getNonTransitMode());
        return s1.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return options.modes.contains(TraverseMode.BICYCLE) ? 0 : Double.POSITIVE_INFINITY;
    }

    public String toString() {
        return "StreetBikeRentalLink(" + fromv + " -> " + tov + ")";
    }
}
