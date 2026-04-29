package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import java.util.Optional;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;

class GenericLocationMapper {

  private final FeedScopedIdMapper idMapper;

  GenericLocationMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  /// Maps a GraphQL Location input type to a GenericLocation.
  /// Returns an empty result If the input does not contain a coordinate or an id.
  Optional<GenericLocation> toGenericLocation(Map<String, Object> m) {
    Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
    Double lat = null;
    Double lon = null;
    if (coordinates != null) {
      lat = (Double) coordinates.get("latitude");
      lon = (Double) coordinates.get("longitude");
    }

    String placeRef = (String) m.get("place");
    FeedScopedId stopId = idMapper.parseNullSafe(placeRef).orElse(null);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    if (stopId != null && lat != null && lon != null) {
      return Optional.of(GenericLocation.fromStopIdWithFallback(stopId, lat, lon, name));
    } else if (stopId != null) {
      return Optional.of(GenericLocation.fromStopId(stopId, name));
    } else if (lat != null && lon != null) {
      return Optional.of(GenericLocation.fromCoordinate(lat, lon, name));
    } else {
      return Optional.empty();
    }
  }
}
