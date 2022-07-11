package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.TransitModel;

public class DigitransitStopPropertyMapper extends PropertyMapper<TransitStopVertex> {

  private final TransitModel transitModel;

  private DigitransitStopPropertyMapper(TransitModel transitModel) {
    this.transitModel = transitModel;
  }

  public static DigitransitStopPropertyMapper create(TransitModel transitModel) {
    return new DigitransitStopPropertyMapper(transitModel);
  }

  @Override
  public Collection<T2<String, Object>> map(TransitStopVertex input) {
    Stop stop = input.getStop();
    Collection<TripPattern> patternsForStop = transitModel.index.getPatternsForStop(stop);

    String type = patternsForStop
      .stream()
      .map(TripPattern::getMode)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .map(Enum::name)
      .orElse(null);

    String patterns = JSONArray.toJSONString(
      patternsForStop
        .stream()
        .map(tripPattern -> {
          String headsign = tripPattern.getStopHeadsign(tripPattern.findStopPosition(stop));
          JSONObject pattern = new JSONObject();
          pattern.put("headsign", Optional.ofNullable(headsign).orElse(""));
          pattern.put("type", tripPattern.getRoute().getMode().name());
          pattern.put("shortName", tripPattern.getRoute().getShortName());
          return pattern;
        })
        .collect(Collectors.toList())
    );
    String desc = stop.getDescription() != null ? stop.getDescription().toString() : null;
    return List.of(
      new T2<>("gtfsId", stop.getId().toString()),
      // Name is I18NString now, we return default name
      new T2<>("name", stop.getName().toString()),
      new T2<>("code", stop.getCode()),
      new T2<>("platform", stop.getPlatformCode()),
      new T2<>("desc", desc),
      new T2<>(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : "null"
      ),
      new T2<>("type", type),
      new T2<>("patterns", patterns)
    );
  }
}
