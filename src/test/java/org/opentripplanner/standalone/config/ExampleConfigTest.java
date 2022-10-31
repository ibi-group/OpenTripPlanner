package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentest4j.AssertionFailedError;
import org.opentripplanner.standalone.config.framework.JsonSupport;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.project.EnvironmentVariableReplacer;
import org.opentripplanner.test.support.FilePatternSource;

public class ExampleConfigTest {

  @FilePatternSource(pattern = "docs/examples/**/router-config.json")
  @ParameterizedTest(name = "Check validity of {0}")
  void routerConfig(Path filename) {
    testConfig(filename, a -> new RouterConfig(a, true));
  }

  @FilePatternSource(
    pattern = { "docs/examples/**/build-config.json", "test/performance/**/build-config.json" }
  )
  @ParameterizedTest(name = "Check validity of {0}")
  void buildConfig(Path filename) {
    testConfig(filename, a -> new BuildConfig(a, true));
  }

  @FilePatternSource(pattern = { "src/test/resources/standalone/config/invalid-config.json" })
  @ParameterizedTest(name = "Fail when parsing an invalid config from {0}")
  void failInvalidConfig(Path filename) {
    Assertions.assertThrows(
      AssertionFailedError.class,
      () -> testConfig(filename, a -> new BuildConfig(a, true))
    );
  }

  private void testConfig(Path path, Consumer<NodeAdapter> buildConfig) {
    try {
      var json = Files.readString(path);
      var replaced = EnvironmentVariableReplacer.insertVariables(
        json,
        json,
        ignored -> "some-value"
      );
      var node = JsonSupport.jsonNodeFromString(replaced);
      var a = new NodeAdapter(node, getClass().getSimpleName());
      buildConfig.accept(a);

      // Test for unused parameters
      var buf = new StringBuilder();
      a.logAllUnusedParameters(m -> buf.append("\n").append(m));
      if (!buf.isEmpty()) {
        fail(buf.toString());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
