package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeForTest;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromResource;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class BuildConfigTest {

  private static final String SOURCE = "BuildConfigTest";

  /**
   * Test that the build-config.json example used in documentation is valid.
   */
  @Test
  void validateExample() {
    var node = jsonNodeFromResource("standalone/config/build-config.json");

    // Setup so we get access to the NodeAdapter
    var a = new NodeAdapter(node, SOURCE);
    var c = new BuildConfig(a, false);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllUnusedParameters(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }

  @Test
  public void boardingLocationRefs() {
    var node = jsonNodeForTest("{ 'boardingLocationTags' : ['a-ha', 'royksopp'] }");

    var subject = new BuildConfig(node, "Test", false);

    assertEquals(Set.of("a-ha", "royksopp"), subject.boardingLocationTags);
  }

  @Test
  public void fareService() {
    var node = jsonNodeForTest("{ 'fares' : \"highestFareInFreeTransferWindow\" }");
    var conf = new BuildConfig(node, "Test", false);
    assertInstanceOf(DefaultFareService.class, conf.fareServiceFactory.makeFareService());
  }
}
