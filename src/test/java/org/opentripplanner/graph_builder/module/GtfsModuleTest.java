package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.TemporaryGraphBuildData;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;

class GtfsModuleTest {

    @Test
    public void extractMinTransfers() throws URISyntaxException {
        var bundle = new GtfsBundle(FakeGraph.getFileForResource("/testagency.zip"));
        var module = new GtfsModule(List.of(bundle), ServiceDateInterval.unbounded());
        var graph = new Graph();

        var extra = new TemporaryGraphBuildData();

        module.buildGraph(graph, extra);

        var minTimeTransfers = extra.getMinTimeTransfers();

        assertEquals(2, minTimeTransfers.size());

        var transfer = minTimeTransfers.get(1);
        assertEquals(transfer.minTransferTime, Duration.ofSeconds(13000));
    }
}