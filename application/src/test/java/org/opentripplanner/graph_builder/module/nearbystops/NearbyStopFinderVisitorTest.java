package org.opentripplanner.graph_builder.module.nearbystops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.RegularStop;

class NearbyStopFinderVisitorTest {

  private static final TransitTestEnvironmentBuilder ENV = TransitTestEnvironment.of();
  static final RegularStop STOP = ENV.stop("stop-1");
  static final FeedScopedId AREA_STOP_ID = new FeedScopedId("A", "area-1");

  @Test
  void collectsTransitStops() {
    var visitor = new NearbyStopFinderVisitor(Set.of(), Set.of(), false);

    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    visitor.visitVertex(state);

    var expected = NearbyStop.nearbyStopForState(state, STOP.getId());
    assertEquals(List.of(expected), visitor.transitStopsFound());
  }

  @Test
  void skipsOriginVertices() {
    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    var originVertices = Set.of(state.getVertex());
    var visitor = new NearbyStopFinderVisitor(originVertices, Set.of(), false);

    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void skipsIgnoreVertices() {
    var state = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();
    var ignoreVertices = Set.of(state.getVertex());
    var visitor = new NearbyStopFinderVisitor(Set.of(), ignoreVertices, false);

    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void skipsNonTransitVertices() {
    var visitor = new NearbyStopFinderVisitor(Set.of(), Set.of(), false);

    // State at a regular intersection, not a transit stop
    var state = TestStateBuilder.ofWalking().streetEdge().build();
    visitor.visitVertex(state);

    assertTrue(visitor.transitStopsFound().isEmpty());
  }

  @Test
  void collectsAreaStopsWhenFlexEnabled() {
    OTPFeature.FlexRouting.testOn(() -> {
      var vertex = StreetModelForTest.intersectionVertex(10, 10);
      var other = StreetModelForTest.intersectionVertex(10.1, 10.1);
      StreetModelForTest.streetEdge(vertex, other, StreetTraversalPermission.CAR);
      vertex.addAreaStops(Set.of(AREA_STOP_ID));

      var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
      var visitor = new NearbyStopFinderVisitor(Set.of(), Set.of(), false);
      visitor.visitVertex(state);

      assertEquals(1, visitor.statesForAreaStopIds().size());
      assertTrue(visitor.statesForAreaStopIds().containsKey(AREA_STOP_ID));
      assertEquals(state, visitor.statesForAreaStopIds().get(AREA_STOP_ID).iterator().next());
    });
  }

  @Test
  void doesNotCollectAreaStopsWithoutCarPermission() {
    OTPFeature.FlexRouting.testOn(() -> {
      var vertex = StreetModelForTest.intersectionVertex(20, 20);
      var other = StreetModelForTest.intersectionVertex(20.1, 20.1);
      StreetModelForTest.streetEdge(vertex, other, StreetTraversalPermission.PEDESTRIAN);
      vertex.addAreaStops(Set.of(AREA_STOP_ID));

      var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
      var visitor = new NearbyStopFinderVisitor(Set.of(), Set.of(), false);
      visitor.visitVertex(state);

      assertTrue(visitor.statesForAreaStopIds().isEmpty());
    });
  }

  @Test
  void collectsAreaStopsInReverseDirection() {
    OTPFeature.FlexRouting.testOn(() -> {
      var vertex = StreetModelForTest.intersectionVertex(30, 30);
      var other = StreetModelForTest.intersectionVertex(30.1, 30.1);
      // Create incoming CAR edge: other -> vertex
      StreetModelForTest.streetEdge(other, vertex, StreetTraversalPermission.CAR);
      vertex.addAreaStops(Set.of(AREA_STOP_ID));

      var state = new State(vertex, StreetSearchRequest.of().withStartTime(Instant.EPOCH).build());
      var visitor = new NearbyStopFinderVisitor(Set.of(), Set.of(), true);
      visitor.visitVertex(state);

      assertEquals(1, visitor.statesForAreaStopIds().size());
      assertTrue(visitor.statesForAreaStopIds().containsKey(AREA_STOP_ID));
    });
  }
}
