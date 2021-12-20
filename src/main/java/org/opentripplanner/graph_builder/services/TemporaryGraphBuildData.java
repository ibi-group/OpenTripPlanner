package org.opentripplanner.graph_builder.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.opentripplanner.model.transfer.MinTimeTransfer;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Temporary data structure for passing data from one graph build module to another. The data in
 * here does not end up being saved in the graph.
 * <p>
 * Even though there isn't parallel access of this class at the moment (2021-12-20), it's designed
 * to be completely thread-safe to make future parallelisation trivial.
 */
public class TemporaryGraphBuildData {

    private Queue<MinTimeTransfer> minTimeTransfers = new ConcurrentLinkedQueue<>();

    private Map<Vertex, Double> elevationData = new ConcurrentHashMap<>();

    public void addMinTimeTransfers(Collection<MinTimeTransfer> transfer) {
        minTimeTransfers.addAll(transfer);
    }

    public ImmutableList<MinTimeTransfer> getMinTimeTransfers() {
        return ImmutableList.copyOf(minTimeTransfers);
    }

    public void addElevationData(Map<Vertex, Double> data) {
        elevationData.putAll(data);
    }

    public ImmutableMap<Vertex, Double> getElevationData() {
        return ImmutableMap.copyOf(elevationData);
    }
}
