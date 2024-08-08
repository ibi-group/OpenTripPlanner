package org.opentripplanner.ext.mobilityprofile;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that processes CSV files containing profile-based OSM costs.
 */
public class MobilityProfileParser {

  private static final Logger LOG = LoggerFactory.getLogger(MobilityProfileParser.class);

  private static final int ONE_MILE_IN_METERS = 1609;

  private MobilityProfileParser() {}

  /**
   * Process rows from the given CSV stream and build a table indexed by both the
   * upstream/downstream nodes, where each value is a map of costs by mobility profile.
   */
  public static Map<String, MobilityProfileData> parseData(InputStream is) {
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
  public static String getKey(long id, long from, long to) {
    return String.format("%d:%d=>%d", id, from, to);
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
      long osmWayId = Long.parseLong(id, 10);
      String key = getKey(osmWayId, fromNode, toNode);
      float lengthMeters = ONE_MILE_IN_METERS * Float.parseFloat(reader.get("Link Length"));

      var weightMap = new EnumMap<MobilityProfile, Float>(MobilityProfile.class);
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

      map.put(key, new MobilityProfileData(lengthMeters, fromNode, toNode, weightMap));
    } catch (NumberFormatException | NullPointerException e) {
      LOG.warn(
        "Skipping mobility profile data at line {}: missing/invalid data in column {}.",
        lineNumber,
        currentColumnHeader
      );
    }
  }
}
