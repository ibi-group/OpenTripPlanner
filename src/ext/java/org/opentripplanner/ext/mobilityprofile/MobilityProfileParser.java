package org.opentripplanner.ext.mobilityprofile;

import com.csvreader.CsvReader;
import com.google.common.collect.ImmutableTable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobilityProfileParser {

  private record MobilityProfileEntry(
    String timestamp, // Not actively used, so processed as text for reference.
    String fromNode,
    String toNode,
    double link,
    Map<String, Double> weightByProfile
  ) {}

  public static Map<String, ImmutableTable<String, String, Double>> parseData(InputStream is) {
    List<String> profileNames = List.of(
      "None",
      "Some",
      "Device",
      "WChairM",
      "WChairE",
      "MScooter",
      "Vision",
      "Vision+",
      "Some-Vision",
      "Device-Vision",
      "WChairM-Vision",
      "WChairE-Vision",
      "MScooter-Vision",
      "Some-Vision+",
      "Device-Vision+",
      "WChairM-Vision+",
      "WChairE-Vision+",
      "MScooter-Vision+"
    );

    try {
      var reader = new CsvReader(is, StandardCharsets.UTF_8);
      reader.setDelimiter(',');

      reader.readHeaders();

      // Process rows from the CSV file first.
      var entries = new ArrayList<MobilityProfileEntry>();
      while (reader.readRecord()) {
        String timestamp = reader.get("Timestamp");
        String fromNode = reader.get("Upstream Node");
        String toNode = reader.get("Downstream Node");
        double linkWeight = Double.parseDouble(reader.get("Link"));
        var weightMap = new HashMap<String, Double>();
        for (var profile : profileNames) {
          weightMap.put(profile, Double.parseDouble(reader.get(profile)));
        }
        var entry = new MobilityProfileEntry(
          timestamp,
          fromNode,
          toNode,
          linkWeight,
          weightMap
        );
        entries.add(entry);
      }

      // Build a map indexed by profile, where each value for a given profile is
      // a table of weights indexed by both the from and the to node.
      var processedMobilityData = new HashMap<String, ImmutableTable<String, String, Double>>();
      for (var profile : profileNames) {
        ImmutableTable.Builder<String, String, Double> tableBuilder = ImmutableTable.builder();
        for (var entry : entries) {
          tableBuilder.put(entry.fromNode, entry.toNode, entry.weightByProfile.get(profile));
        }
        processedMobilityData.put(profile, tableBuilder.build());
      }

      return processedMobilityData;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
