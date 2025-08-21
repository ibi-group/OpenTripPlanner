package org.opentripplanner.graph_builder.module.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.SiteRepository;

class GeometryProcessorTest {

  private static final FeedScopedId SHAPE_ID = id("s1");
  private static final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private static final SiteRepository repo = testModel.siteRepositoryBuilder().build();

  @Test
  void ignoreInvalidReference() {
    var issueStore = new DefaultDataImportIssueStore();
    var builder = new OtpTransitServiceBuilder(repo, issueStore);
    builder
      .getShapePoints()
      .put(SHAPE_ID, List.of(new ShapePoint(0, 0, 0, 0.0), new ShapePoint(1, 1, 1, 1.0)));

    var invalidRef = id("unknown");
    var trip = TimetableRepositoryForTest.trip("t").withShapeId(invalidRef).build();

    var stopTimes = IntStream.range(0, 3)
      .mapToObj(index -> testModel.stopTime(trip, index, testModel.stop("s" + index).build()))
      .toList();
    builder.getStopTimesSortedByTrip().put(trip, stopTimes);

    var processor = new GeometryProcessor(builder, 150, issueStore);
    var linestrings = processor.createHopGeometries(trip);

    assertThat(linestrings).hasSize(2);

    assertEquals(
      "[Issue{type: 'InvalidShapeReference', message: 'Trip 'F:t' refers to unknown shape geometry 'F:unknown''}]",
      issueStore.listIssues().toString()
    );
  }
}
