package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedId;
import org.opentripplanner.model.Stop;

public class StopAgencyAndIdAdapter extends XmlAdapter<AgencyAndIdType, Stop> {

    @Override
    public Stop unmarshal(AgencyAndIdType arg) throws Exception {
        throw new UnsupportedOperationException(
                "We presently serialize stops as FeedId, and thus cannot deserialize them");
    }

    @Override
    public AgencyAndIdType marshal(Stop arg) throws Exception {
        if (arg == null) {
            return null;
        }
        FeedId id = arg.getId();
        return new AgencyAndIdType(id.getAgencyId(), id.getId());
    }

}
