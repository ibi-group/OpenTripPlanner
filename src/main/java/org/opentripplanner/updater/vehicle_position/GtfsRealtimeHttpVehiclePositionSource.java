package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for downloading GTFS-rt vehicle positions from a URL and loading into memory.
 */
public class GtfsRealtimeHttpVehiclePositionSource {

  private static final Logger LOG = LoggerFactory.getLogger(
    GtfsRealtimeHttpVehiclePositionSource.class
  );

  /**
   * URL to grab GTFS-RT feed from
   */
  private final URI url;

  private final HttpHeaders headers;

  public GtfsRealtimeHttpVehiclePositionSource(URI url, HttpHeaders headers) {
    this.url = url;
    this.headers = HttpHeaders.of().acceptProtobuf().add(headers).build();
  }

  /**
   * Parses raw GTFS-RT data into vehicle positions
   */
  public List<VehiclePosition> getPositions() {
    try (InputStream is = HttpUtils.openInputStream(url.toString(), headers.asMap())) {
      if (is == null) {
        LOG.warn("Failed to get data from url {}", url);
        return List.of();
      }
      return this.getPositions(is);
    } catch (IOException e) {
      LOG.warn("Error reading vehicle positions from {}", url, e);
    }
    return List.of();
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(GtfsRealtimeHttpVehiclePositionSource.class)
      .addObj("url", url)
      .toString();
  }

  public List<VehiclePosition> getPositions(InputStream is) throws IOException {
    List<VehiclePosition> positions = null;
    List<GtfsRealtime.FeedEntity> feedEntityList;
    GtfsRealtime.FeedMessage feedMessage;

    if (is != null) {
      // Decode message
      feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
      feedEntityList = feedMessage.getEntityList();

      // Create List of TripUpdates
      positions = new ArrayList<>(feedEntityList.size());
      for (GtfsRealtime.FeedEntity feedEntity : feedEntityList) {
        if (feedEntity.hasVehicle()) {
          positions.add(feedEntity.getVehicle());
        }
      }
    }

    return positions;
  }
}
