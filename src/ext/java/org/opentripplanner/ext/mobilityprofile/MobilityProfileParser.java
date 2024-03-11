package org.opentripplanner.ext.mobilityprofile;

import com.csvreader.CsvReader;
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
  public static Map<String, MobilityProfileData> parseData(
    InputStream is
  ) {
    try {
      var reader = new CsvReader(is, StandardCharsets.UTF_8);
      reader.setDelimiter(',');
      reader.readHeaders();

      Map<String, MobilityProfileData> map = new HashMap<>();
      int lineNumber = 1;
      while (reader.readRecord()) {
        parseRow(lineNumber, reader, map);
        lineNumber++;
      }

      LOG.info("Imported {} rows from mobility-profile.csv", map.size());
      return map;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Helper to build a key of the form "id:from=>to" for an OSM way. */
  public static String getKey(String id, String from, String to) {
    return String.format("%s:%s=>%s", id, from, to);
  }

  private static void parseRow(
    int lineNumber,
    CsvReader reader,
    Map<String, MobilityProfileData> map
  ) throws IOException {
    String currentColumnHeader = "";
    try {
      long fromNode = Long.parseLong(reader.get("Upstream Node"), 10);
      long toNode = Long.parseLong(reader.get("Downstream Node"), 10);
      String id = reader.get("Way Id");
      float length = Float.parseFloat(reader.get("Link Length"));

      // The weight map has to be a HashMap instead of an EnumMap so that it is correctly
      // persisted in the graph.
      var weightMap = new HashMap<MobilityProfile, Float>();
      for (var profile : MobilityProfile.values()) {
        currentColumnHeader = profile.getText();
        try {
          weightMap.put(profile, Float.parseFloat(reader.get(currentColumnHeader)));
        } catch (NumberFormatException | NullPointerException e) {
          LOG.warn(
            "Ignoring missing/invalid data at line {}, column {}.",
            lineNumber,
            currentColumnHeader
          );
        }
      }

      map.put(
        getKey(
          id,
          VertexLabel.osm(fromNode).toString(),
          VertexLabel.osm(toNode).toString()
        ),
        new MobilityProfileData(length, fromNode, toNode, weightMap)
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
