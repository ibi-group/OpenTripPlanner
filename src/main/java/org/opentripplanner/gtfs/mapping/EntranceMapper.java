package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslationHelper;

/**
 * Responsible for mapping GTFS Entrance into the OTP model.
 */
class EntranceMapper {

    private Map<org.onebusaway.gtfs.model.Stop, Entrance> mappedEntrances = new HashMap<>();

    Collection<Entrance> map(Collection<org.onebusaway.gtfs.model.Stop> allEntrances) {
        return MapUtils.mapToList(allEntrances, this::map);
    }

    /**
     * Map from GTFS to OTP model, {@code null} safe.
     */
    Entrance map(org.onebusaway.gtfs.model.Stop original) {
        return map(original, null);
    }

    Entrance map(org.onebusaway.gtfs.model.Stop original, TranslationHelper translationHelper) {
        return original == null
                ? null
                : mappedEntrances.computeIfAbsent(
                        original, k -> doMap(original, translationHelper));
    }

    private Entrance doMap(
            org.onebusaway.gtfs.model.Stop gtfsStop,
            TranslationHelper translationHelper
    ) {
        if (gtfsStop.getLocationType()
                != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
            throw new IllegalArgumentException(
                    "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT
                            + ", but got " + gtfsStop.getLocationType());
        }

        StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

        if (translationHelper != null) {
            return new Entrance(
                    base.getId(),
                    translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                            TranslationHelper.STOP_NAME, base.getId().getId(),
                            null, base.getName()
                    ),
                    base.getCode(),
                    base.getDescription(),
                    base.getCoordinate(),
                    base.getWheelchairBoarding(),
                    base.getLevel()
            );
        }

        return new Entrance(
                base.getId(),
                new NonLocalizedString(base.getName()),
                base.getCode(),
                base.getDescription(),
                base.getCoordinate(),
                base.getWheelchairBoarding(),
                base.getLevel()
        );
    }
}
