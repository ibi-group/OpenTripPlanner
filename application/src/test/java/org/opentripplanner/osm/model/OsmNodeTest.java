package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OsmNodeTest {

  public static Stream<Arguments> entranceTestCases() {
    var mainEntrance = NodeBuilder.of().withTag("entrance", "main").build();
    var trainStationEntrance = NodeBuilder.of()
      .withTag("railway", "train_station_entrance")
      .build();
    var subwayEntrance = NodeBuilder.of().withTag("railway", "subway_entrance").build();
    var publicTransportEntrance = NodeBuilder.of().withTag("public_transport", "entrance").build();
    var trainStationEntranceWithEntranceTag = NodeBuilder.of()
      .withTag("railway", "train_station_entrance")
      .withTag("entrance", "main")
      .build();
    var notAnEntrance = NodeBuilder.of().withTag("entrance", "no").build();
    var emergencyEntrance = NodeBuilder.of().withTag("entrance", "emergency").build();

    return Stream.of(
      Arguments.argumentSet("main entrance", mainEntrance, true, false),
      Arguments.argumentSet("train station entrance", trainStationEntrance, true, true),
      Arguments.argumentSet("subway entrance", subwayEntrance, true, true),
      Arguments.argumentSet("public transport entrance", publicTransportEntrance, true, true),
      Arguments.argumentSet(
        "train station entrance with entrance tag",
        trainStationEntranceWithEntranceTag,
        true,
        true
      ),
      Arguments.argumentSet("not an entrance", notAnEntrance, false, false),
      Arguments.argumentSet("emergency entrance", emergencyEntrance, false, false)
    );
  }

  @Test
  public void isBarrier() {
    OsmNode node = new OsmNode();
    assertFalse(node.isBarrier());

    node = new OsmNode();
    node.addTag("barrier", "unknown");
    assertFalse(node.isBarrier());

    node = new OsmNode();
    node.addTag("barrier", "bollard");
    assertTrue(node.isBarrier());

    node = new OsmNode();
    node.addTag("access", "no");
    assertTrue(node.isBarrier());
  }

  @Test
  public void isTaggedBarrierCrossing() {
    OsmNode node = new OsmNode();
    assertFalse(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("barrier", "gate");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("motor_vehicle", "yes");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("motor_vehicle", "no");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("access", "yes");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("access", "no");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("entrance", "main");
    assertTrue(node.isTaggedBarrierCrossing());
  }

  @ParameterizedTest
  @MethodSource("entranceTestCases")
  public void isEntrance(OsmNode node, boolean entrance, boolean stationEntrance) {
    assertEquals(entrance, node.isEntrance());
    assertEquals(stationEntrance, node.isStationEntrance());
  }
}
