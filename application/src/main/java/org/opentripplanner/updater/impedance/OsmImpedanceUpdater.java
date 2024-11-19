package org.opentripplanner.updater.impedance;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileData;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileParser;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * G-MAP OSM impedance updater
 */
public class OsmImpedanceUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(OsmImpedanceUpdater.class);

  private final String url;
  private final ImpedanceUpdateHandler updateHandler;
  private final HttpHeaders headers;
  private final OtpHttpClient otpHttpClient;
  private WriteToGraphCallback saveResultOnGraph;
  private Map<String, MobilityProfileData> previousImpedances = Map.of();

  public OsmImpedanceUpdater(OsmImpedanceUpdaterParameters config) {
    super(config);
    this.url = config.url();
    this.headers = HttpHeaders.of().add(config.headers()).build();

    this.updateHandler = new ImpedanceUpdateHandler();
    this.otpHttpClient = new OtpHttpClientFactory().create(LOG);
    LOG.info("Creating impedance updater running every {}: {}", pollingPeriod(), url);
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addStr("url", url).toString();
  }

  @Override
  protected void runPolling() {
    LOG.info("Fetching mobility impedances...");
    try {
      final Map<String, MobilityProfileData> impedances = otpHttpClient.getAndMap(
        URI.create(url),
        this.headers.asMap(),
        MobilityProfileParser::parseData
      );
      LOG.info("Fetched mobility impedances.");

      // Filter out which rows have been updated since previous poll.
      Map<String, MobilityProfileData> changedImpedances = getChangedImpedances(
        impedances,
        previousImpedances
      );
      previousImpedances = impedances;

      // Handle update in graph writer runnable
      if (!changedImpedances.isEmpty()) {
        saveResultOnGraph.execute((graph, transitModel) ->
          updateHandler.update(graph, changedImpedances)
        );
      } else {
        LOG.error("Impedance data unchanged (not updating graph).");
      }
    } catch (Exception e) {
      // Download errors, including timeouts, will be caught here.
      LOG.error("Error parsing impedance data from {}", url, e);
    }
  }

  /**
   * Indicates whether two profile data have the same impedances.
   */
  public static boolean areSameImpedances(
    @Nonnull MobilityProfileData profileData1,
    @Nonnull MobilityProfileData profileData2
  ) {
    return profileData1.equals(profileData2);
  }

  /**
   * Performs a diff with existing entries, to avoid updating unchanged portions of the graph.
   */
  public static Map<String, MobilityProfileData> getChangedImpedances(
    @Nonnull Map<String, MobilityProfileData> newImpedances,
    @Nonnull Map<String, MobilityProfileData> existingImpedances
  ) {
    Map<String, MobilityProfileData> result = new HashMap<>();

    for (var entry : newImpedances.entrySet()) {
      String key = entry.getKey();

      // Include entries that exist in both sets and that were modified in newImpedances.
      // Include entries introduced in newImpedances not in existingImpedances.
      var existingImpedance = existingImpedances.get(key);
      if (existingImpedance == null || !areSameImpedances(entry.getValue(), existingImpedance)) {
        result.put(key, entry.getValue());
      }
    }

    for (var entry : existingImpedances.entrySet()) {
      String key = entry.getKey();

      // Include entries that were removed in newImpedances, but mark them as empty map.
      if (!newImpedances.containsKey(key)) {
        MobilityProfileData removedData = entry.getValue();
        result.put(
          key,
          new MobilityProfileData(
            removedData.lengthInMeters(),
            removedData.fromNode(),
            removedData.toNode(),
            Map.of()
          )
        );
      }
    }

    return result;
  }
}
