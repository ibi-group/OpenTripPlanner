package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.StreetVertex;

public abstract class ParkAndRideTest extends GraphRoutingTest {

  protected Graph graph;

  public static List<String> graphPath(GraphPath graphPath) {
    return graphPath.states
      .stream()
      .map(s ->
        String.format(
          "%s%s - %s (%,.2f, %d)",
          s.getBackMode(),
          s.isVehicleParked() ? " (parked)" : "",
          s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null,
          s.getWeight(),
          s.getElapsedTimeSeconds()
        )
      )
      .collect(Collectors.toList());
  }

  protected void assertPathWithParking(
    StreetVertex fromVertex,
    StreetVertex toVertex,
    StreetMode streetMode,
    Set<String> bannedTags,
    Set<String> requiredTags
  ) {
    assertNotEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        false
      )
    );
    assertNotEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        true
      )
    );
  }

  protected void assertNoPathWithParking(
    StreetVertex fromVertex,
    StreetVertex toVertex,
    StreetMode streetMode,
    Set<String> bannedTags,
    Set<String> requiredTags
  ) {
    assertEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        false
      )
    );
    assertEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        true
      )
    );
  }

  protected void assertEmptyPath(Vertex fromVertex, Vertex toVertex, StreetMode streetMode) {
    assertPath(fromVertex, toVertex, streetMode, false, List.of(), List.of());
  }

  protected void assertEmptyPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible
  ) {
    assertPath(fromVertex, toVertex, streetMode, requireWheelChairAccessible, List.of(), List.of());
  }

  protected void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    String... descriptor
  ) {
    assertPath(fromVertex, toVertex, streetMode, false, List.of(descriptor), List.of(descriptor));
  }

  protected void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    String... descriptor
  ) {
    assertPath(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      List.of(descriptor),
      List.of(descriptor)
    );
  }

  protected List<String> runStreetSearchAndCreateDescriptor(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    Set<String> bannedTags,
    Set<String> requiredTags,
    boolean arriveBy
  ) {
    var options = new RouteRequest();
    var preferences = options.preferences();

    preferences.withBike(it -> it.setParkCost(120).setParkTime(60));
    preferences.car().setParkCost(240);
    preferences.car().setParkTime(180);
    options.setWheelchair(requireWheelChairAccessible);
    options.journey().parking().setBannedTags(bannedTags);
    options.journey().parking().setRequiredTags(requiredTags);
    options.setArriveBy(arriveBy);

    var tree = AStarBuilder
      .oneToOne()
      .setRequest(options)
      .setStreetRequest(new StreetRequest(streetMode))
      .setFrom(fromVertex)
      .setTo(toVertex)
      .getShortestPathTree();

    var path = tree.getPath(arriveBy ? fromVertex : toVertex);

    if (path == null) {
      return List.of();
    }

    return path.states
      .stream()
      .map(s ->
        String.format(
          Locale.ROOT,
          "%s%s - %s (%,.2f, %d)",
          s.getBackMode(),
          s.isVehicleParked() ? " (parked)" : "",
          s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null,
          s.getWeight(),
          s.getElapsedTimeSeconds()
        )
      )
      .collect(Collectors.toList());
  }

  private void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    List<String> departAtDescriptor,
    List<String> arriveByDescriptor
  ) {
    var departAt = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      Set.of(),
      Set.of(),
      false
    );
    var arriveBy = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      Set.of(),
      Set.of(),
      true
    );

    assertEquals(departAtDescriptor, departAt, "departAt path");
    assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
  }
}
