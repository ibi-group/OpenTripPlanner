package org.opentripplanner.routing.impl;

import com.google.common.collect.Lists;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Calculate Orca discount fares based on the fare type and the agencies traversed within a trip.
 */
public class OrcaFareServiceImpl extends DefaultFareServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(OrcaFareServiceImpl.class);

    private static final long FREE_TRANSFER_TIME_DURATION = 7200; // 2 hours

    public static final String COMM_TRANS_AGENCY_ID = "29";
    public static final String KC_METRO_AGENCY_ID = "1";
    public static final String SOUND_TRANSIT_AGENCY_ID = "40";
    public static final String EVERETT_TRANSIT_AGENCY_ID = "97";
    public static final String PIERCE_COUNTY_TRANSIT_AGENCY_ID = "3";
    public static final String SKAGIT_TRANSIT_AGENCY_ID = "e0e4541a-2714-487b-b30c-f5c6cb4a310f";
    public static final String SEATTLE_STREET_CAR_AGENCY_ID = "23";
    public static final String WASHINGTON_STATE_FERRIES_AGENCY_ID = "wsf";
    public static final String KITSAP_TRANSIT_AGENCY_ID = "kt";
    public static final int ROUTE_TYPE_FERRY = 4;

    public enum RideType {
        COMM_TRANS_LOCAL_SWIFT,
        COMM_TRANS_COMMUTER_EXPRESS,
        EVERETT_TRANSIT,
        KC_WATER_TAXI_VASHON_ISLAND,
        KC_WATER_TAXI_WEST_SEATTLE,
        KC_METRO,
        KITSAP_TRANSIT,
        PIERCE_COUNTY_TRANSIT,
        SKAGIT_TRANSIT,
        SEATTLE_STREET_CAR,
        SOUND_TRANSIT,
        WASHINGTON_STATE_FERRIES
    }

    private static final Map<String, Function<Route, RideType>> classificationStrategy = new HashMap<>();

    // If set to true, the test ride price is used instead of the actual agency cash fare.
    public boolean IS_TEST;
    public static final float DEFAULT_TEST_RIDE_PRICE = 3.49f;

    public OrcaFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(Fare.FareType.regular, regularFareRules);
        addFareRules(Fare.FareType.senior, regularFareRules);
        addFareRules(Fare.FareType.youth, regularFareRules);
        addFareRules(Fare.FareType.electronicRegular, regularFareRules);
        addFareRules(Fare.FareType.electronicYouth, regularFareRules);
        addFareRules(Fare.FareType.electronicSpecial, regularFareRules);
        addFareRules(Fare.FareType.electronicSenior, regularFareRules);

        classificationStrategy.put(
            COMM_TRANS_AGENCY_ID,
            routeData -> {
                try {
                    int routeId = Integer.parseInt(routeData.getShortName());
                    if (routeId >= 400 && routeId <= 899) {
                        return RideType.COMM_TRANS_COMMUTER_EXPRESS;
                    }
                    return RideType.COMM_TRANS_LOCAL_SWIFT;
                } catch (NumberFormatException e) {
                    LOG.warn("Unable to determine comm trans route id from {}.", routeData.getShortName(), e);
                    return RideType.COMM_TRANS_LOCAL_SWIFT;
                }
            }
        );
        classificationStrategy.put(
            KC_METRO_AGENCY_ID,
            routeData -> {
                if (routeData.getType() == ROUTE_TYPE_FERRY &&
                    routeData.getDesc().contains("Water Taxi: West Seattle")) {
                    return RideType.KC_WATER_TAXI_WEST_SEATTLE;
                } else if (routeData.getType() == ROUTE_TYPE_FERRY &&
                    routeData.getDesc().contains("Water Taxi: Vashon Island")) {
                    return RideType.KC_WATER_TAXI_VASHON_ISLAND;
                }
                return RideType.KC_METRO;
            }
        );
        classificationStrategy.put(SOUND_TRANSIT_AGENCY_ID, routeData -> RideType.SOUND_TRANSIT);
        classificationStrategy.put(EVERETT_TRANSIT_AGENCY_ID, routeData -> RideType.EVERETT_TRANSIT);
        classificationStrategy.put(PIERCE_COUNTY_TRANSIT_AGENCY_ID, routeData -> RideType.PIERCE_COUNTY_TRANSIT);
        classificationStrategy.put(SKAGIT_TRANSIT_AGENCY_ID, routeData -> RideType.SKAGIT_TRANSIT);
        classificationStrategy.put(SEATTLE_STREET_CAR_AGENCY_ID, routeData -> RideType.SEATTLE_STREET_CAR);
        classificationStrategy.put(WASHINGTON_STATE_FERRIES_AGENCY_ID, routeData -> RideType.WASHINGTON_STATE_FERRIES);
        classificationStrategy.put(KITSAP_TRANSIT_AGENCY_ID, routeData -> RideType.KITSAP_TRANSIT);
    }

    /**
     * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. In
     * some cases the route description and short name are needed to define inner agency ride types.
     */
    private static RideType classify(Route routeData) {
        Function<Route, RideType> classifier = classificationStrategy.get(routeData.getAgency().getId());
        return classifier != null ? classifier.apply(routeData) : null;
    }

    /**
     * Define which discount fare should be applied based on the fare type. If the ride type is unknown the discount
     * fare can not be applied, use the default fare.
     */
    private float getDiscountedFare(Fare.FareType fareType, RideType rideType, float defaultFare) {
        if (rideType == null) {
            return defaultFare;
        }
        switch (fareType) {
            case youth:
            case electronicYouth:
                return getYouthFare(rideType, defaultFare);
            case electronicSpecial:
                return getLiftFare(rideType, defaultFare);
            case electronicSenior:
            case senior:
                return getSeniorFare(fareType, rideType, defaultFare);
            case electronicRegular:
                return getRegularFare(rideType, defaultFare);
            case regular:
            default:
                return defaultFare;
        }
    }

    /**
     * Apply Orca regular discount fares. If the ride type can not be matched the default fare is used.
     */
    private float getRegularFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case KC_WATER_TAXI_VASHON_ISLAND: return 5.75f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 5.00f;
            default: return defaultFare;
        }
    }

    /**
     * Apply Orca lift discount fares based on the ride type.
     */
    private float getLiftFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KITSAP_TRANSIT: return 1.00f;
            case KC_METRO:
            case SOUND_TRANSIT:
            case EVERETT_TRANSIT:
            case SEATTLE_STREET_CAR:
                return 1.50f;
            case PIERCE_COUNTY_TRANSIT:
            default:
                return defaultFare;
        }
    }

    /**
     * Apply Orca senior discount fares based on the fare and ride types.
     */
    private float getSeniorFare(Fare.FareType fareType, RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case EVERETT_TRANSIT:
                return fareType.equals(Fare.FareType.electronicSenior) ? 0.50f : defaultFare;
            case PIERCE_COUNTY_TRANSIT:
            case SEATTLE_STREET_CAR:
            case KITSAP_TRANSIT:
                // Pierce, Seattle Streetcar, and Kitsap only provide discounted senior fare for orca.
                return fareType.equals(Fare.FareType.electronicSenior) ? 1.00f : defaultFare;
            case KC_WATER_TAXI_VASHON_ISLAND: return 3.00f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 2.50f;
            case KC_METRO:
            case SOUND_TRANSIT:
                return 1.00f;
            default: return defaultFare;
        }
    }

    /**
     * Apply Orca youth discount fares based on the ride type.
     */
    private float getYouthFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.75f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 3.00f;
            case PIERCE_COUNTY_TRANSIT: return 1.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KITSAP_TRANSIT: return 2.00f;
            case KC_METRO:
            case SOUND_TRANSIT:
            case EVERETT_TRANSIT:
            case SEATTLE_STREET_CAR:
                return 1.50f;
            default: return defaultFare;
        }
    }

    /**
     * Get the ride price for a single leg. If testing, this class is being called directly so the required agency cash
     * values are not available therefore the default test price is used instead.
     */
    private float getRidePrice(Ride ride, Fare.FareType fareType, Collection<FareRuleSet> fareRules) {
        if (IS_TEST) {
            // Testing, return default test ride price.
            return DEFAULT_TEST_RIDE_PRICE;
        }
        return calculateCost(fareType, Lists.newArrayList(ride), fareRules);
    }

    /**
     * Calculate the cost of a journey. Where free transfers are not permitted the cash price is used. If free transfers
     * are applicable, the most expensive discount fare across all legs is added to the final cumulative price.
     *
     * The computed fare for Orca card users takes into account realtime trip updates where available, so that, for
     * instance, when a leg on a long itinerary is delayed to begin after the initial two hour window has expired,
     * the calculated fare for that trip will be two one-way fares instead of one.
     */
    @Override
    public boolean populateFare(Fare fare,
                                   Currency currency,
                                   Fare.FareType fareType,
                                   List<Ride> rides,
                                   Collection<FareRuleSet> fareRules
    ) {
        Long freeTransferStartTime = null;
        float cost = 0;
        float orcaFareDiscount = 0;
        for (Ride ride : rides) {
            RideType rideType = classify(ride.routeData);
            if (freeTransferStartTime == null && permitsFreeTransfers(rideType)) {
                // The start of a free transfer must be with a transit agency that permits it!
                freeTransferStartTime = ride.startTime;
            }
            float singleLegPrice = getRidePrice(ride, fareType, fareRules);
            float discountedFare = getDiscountedFare(fareType, rideType, singleLegPrice);
            if (hasFreeTransfers(fareType, rideType) && inFreeTransferWindow(freeTransferStartTime, ride.startTime)) {
                // If using Orca (free transfers), the total fare should be equivalent to the
                // most expensive leg of the journey.
                orcaFareDiscount = Float.max(orcaFareDiscount, discountedFare);
            } else {
                // If free transfers are not permitted, add the cash price of this leg to the total cost.
                cost += singleLegPrice;
            }
            if (!inFreeTransferWindow(freeTransferStartTime, ride.startTime)) {
                // If the trip time has exceeded the free transfer time limit of two hours the rider is required to
                // purchase a new fare. This also resets the free transfer trip window, applies the orca discount to the
                // overall cost and then resets the orca fare discount ready for the next transfer window.
                freeTransferStartTime = null;
                cost += orcaFareDiscount;
                orcaFareDiscount = 0;
            }
        }
        cost += orcaFareDiscount;
        if (cost < Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(currency, cost));
        }
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }

    /**
     *  Trip within the free two hour transfer window.
     */
    private boolean inFreeTransferWindow(Long freeTransferStartTime, long currentLegStartTime) {
        return freeTransferStartTime != null &&
            currentLegStartTime < freeTransferStartTime + FREE_TRANSFER_TIME_DURATION;
    }

    /**
     * A free transfer can be applied if using Orca and the transit agency permits free transfers.
     */
    private boolean hasFreeTransfers(Fare.FareType fareType, RideType rideType) {
        return permitsFreeTransfers(rideType) && usesOrca(fareType);
    }

    /**
     * All transit agencies permit free transfers, apart from these.
     */
    private boolean permitsFreeTransfers(RideType rideType) {
        return rideType != RideType.WASHINGTON_STATE_FERRIES && rideType != RideType.SKAGIT_TRANSIT;
    }

    /**
     * Define Orca fare types.
     */
    private boolean usesOrca(Fare.FareType fareType) {
        return fareType.equals(Fare.FareType.electronicSpecial) ||
            fareType.equals(Fare.FareType.electronicSenior) ||
            fareType.equals(Fare.FareType.electronicRegular) ||
            fareType.equals(Fare.FareType.electronicYouth);
    }
}
