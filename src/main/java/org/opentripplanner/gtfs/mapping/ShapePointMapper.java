package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ShapePointMapper {
    private Map<org.onebusaway.gtfs.model.ShapePoint, ShapePoint> mappedShapePoints = new HashMap<>();

    Collection<ShapePoint> map(Collection<org.onebusaway.gtfs.model.ShapePoint> allShapePoints) {
        return MapUtils.mapToList(allShapePoints, this::map);
    }

    ShapePoint map(org.onebusaway.gtfs.model.ShapePoint orginal) {
        return orginal == null ? null : mappedShapePoints.computeIfAbsent(orginal, this::doMap);
    }

    private ShapePoint doMap(org.onebusaway.gtfs.model.ShapePoint rhs) {
        ShapePoint lhs = new ShapePoint();

        lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
        lhs.setSequence(rhs.getSequence());
        lhs.setLat(rhs.getLat());
        lhs.setLon(rhs.getLon());
        lhs.setDistTraveled(rhs.getDistTraveled());

        // Skip mapping of proxy
        // private transient StopTimeProxy proxy;
        if (rhs.getProxy() != null) {
            throw new IllegalStateException("Did not expect proxy to be set! Data: " + rhs);
        }

        return lhs;
    }
}
