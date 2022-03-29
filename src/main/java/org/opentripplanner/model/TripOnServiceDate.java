package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

/**
 * Class for holding data about a certain trip on a certain day. Essentially a DatedServiceJourney.
 */
public class TripOnServiceDate extends TransitEntity {

    private final Trip trip;
    private final ServiceDate serviceDate;
    private final TripAlteration tripAlteration;

    public TripOnServiceDate(FeedScopedId id, Trip trip, ServiceDate serviceDate, TripAlteration tripAlteration) {
        super(id);
        this.trip = trip;
        this.serviceDate = serviceDate;
        this.tripAlteration = tripAlteration;
    }

    public Trip getTrip() {
        return trip;
    }

    public ServiceDate getServiceDate() {
        return serviceDate;
    }

    public TripAlteration getTripAlteration() {
        return tripAlteration;
    }

}
