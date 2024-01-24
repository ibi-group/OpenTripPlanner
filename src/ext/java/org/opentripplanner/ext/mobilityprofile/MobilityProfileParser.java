package org.opentripplanner.ext.mobilityprofile;

import com.csvreader.CsvReader;
import com.google.common.collect.ImmutableTable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that processes CSV files containing profile-based OSM costs.
 */
public class MobilityProfileParser {

  private static final Logger LOG = LoggerFactory.getLogger(MobilityProfileParser.class);

  private MobilityProfileParser() {}

  /**
   * Process rows from the given CSV stream and build a table indexed by both the
   * upstream/downstream nodes, where each value is a map of costs by mobility profile.
   */
  public static ImmutableTable<String, String, Map<MobilityProfile, Float>> parseData(
    InputStream is
  ) {
    try {
      var reader = new CsvReader(is, StandardCharsets.UTF_8);
      reader.setDelimiter(',');
      reader.readHeaders();

      ImmutableTable.Builder<String, String, Map<MobilityProfile, Float>> tableBuilder = ImmutableTable.builder();
      int lineNumber = 1;
      while (reader.readRecord()) {
        parseRow(lineNumber, reader, tableBuilder);
        lineNumber++;
      }

      return tableBuilder.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void parseRow(
    int lineNumber,
    CsvReader reader,
    ImmutableTable.Builder<String, String, Map<MobilityProfile, Float>> tableBuilder
  ) throws IOException {
    String currentColumnHeader = "";
    try {
      long fromNode = Long.parseLong(reader.get("Upstream Node"), 10);
      long toNode = Long.parseLong(reader.get("Downstream Node"), 10);

      var weightMap = new HashMap<MobilityProfile, Float>();
      for (var profile : MobilityProfile.values()) {
        currentColumnHeader = profile.getText();
        weightMap.put(profile, Float.parseFloat(reader.get(currentColumnHeader)));
      }

      tableBuilder.put(
        VertexLabel.osm(fromNode).toString(),
        VertexLabel.osm(toNode).toString(),
        weightMap
      );
    } catch (NumberFormatException | NullPointerException e) {
      LOG.warn(
        "Skipping mobility profile data at line {}: missing/invalid data in column {}.",
        lineNumber,
        currentColumnHeader
      );
    }
  }
}
