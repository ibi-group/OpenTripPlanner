package org.opentripplanner.routing.fares;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.impl.OrcaFareServiceImpl;
import org.opentripplanner.routing.impl.Ride;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.KC_METRO_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.PIERCE_COUNTY_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.SKAGIT_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.SOUND_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.WASHINGTON_STATE_FERRIES_AGENCY_ID;

public class OrcaFareServiceTest {

    private static OrcaFareServiceImpl orcaFareService;
    private static float DEFAULT_RIDE_PRICE_IN_CENTS;

    @BeforeAll
    public static void setUpClass() {
        Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
        orcaFareService = new OrcaFareServiceImpl(regularFareRules.values());
        orcaFareService.IS_TEST = true;
        DEFAULT_RIDE_PRICE_IN_CENTS = OrcaFareServiceImpl.DEFAULT_TEST_RIDE_PRICE * 100;
    }

    /**
     * These tests are designed to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable
     * to make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
     * substitute {@link OrcaFareServiceImpl#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
     * types.
     */
    private static void calculateFare(List<Ride> rides,
                                  Fare.FareType fareType,
                                  float expectedFareInCents
    ) {
        Fare fare = new Fare();
        orcaFareService.populateFare(fare, null, fareType, rides, null);
        Assertions.assertEquals(expectedFareInCents, fare.getFare(fareType).getCents());
    }

    /**
     * Test to confirm the correct transfer cost per fare type within a single agency.
     */
    @Test
    public void calculateFareForSingleAgency() {
        List<Ride> rides = Collections.singletonList(
            getRide(COMM_TRANS_AGENCY_ID, "400", 0)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.senior, 200f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 200f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 200f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * WSF do not accept free transfers. This test is to make sure the rider is charged the cash price for WSF as well
     * as the highest fare where Orca can be used.
     */
    @Test
    public void calculateFareWithNoFreeTransfer() {
        List<Ride> rides = Arrays.asList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0),
            getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 1),
            getRide(COMM_TRANS_AGENCY_ID, 2)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS + 125);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
        calculateFare(rides, Fare.FareType.electronicRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Check to make sure the fare by leg is calculated properly for a trip with two rides.
     */
    @Test
    public void calculateFareByLeg() {
        List<Ride> rides = Arrays.asList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0),
            getRide(COMM_TRANS_AGENCY_ID, 2)
        );
        Fare fare = new Fare();
        orcaFareService.populateFare(fare, null, Fare.FareType.electronicRegular, rides, null);
        Assertions.assertEquals(349, rides.get(0).fareComponents.get(Fare.FareType.electronicRegular).price.getCents());
        Assertions.assertEquals(0, rides.get(1).fareComponents.get(Fare.FareType.electronicRegular).price.getCents());
        Assertions.assertFalse(rides.get(0).fareComponents.get(Fare.FareType.electronicRegular).isTransfer);
        Assertions.assertTrue(rides.get(1).fareComponents.get(Fare.FareType.electronicRegular).isTransfer);
    }

    /**
     * Total trip time is 2h 30m. The first four transfers are within the permitted two hour window. A single (highest)
     * Orca fare will be charged for these transfers. The fifth transfer is outside of the original two hour window so
     * a single Orca fare for this leg is applied and the two hour window will start again. The final transfer is within
     * the new two hour window and will be free.
     */
    @Test
    public void calculateFareThatExceedsTwoHourFreeTransferWindow() {
        List<Ride> rides = Arrays.asList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 30),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 60),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 90),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 120),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 150)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 100f + 0f + 0f + 0f + 100f + 0f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f +
            DEFAULT_RIDE_PRICE_IN_CENTS + 0f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f + 0f + 0f + 0f + 100f + 0f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }
    /**
     * Total trip time is 2h 30m. Calculate fare with two free transfer windows which include agencies which do not permit
     * free transfers. The free transfers will be applied for Kitsap, but not for WSF nor Skagit. Note: Not a real world
     * trip!
     */
    @Test
    public void calculateFareThatIncludesNoFreeTransfers() {
        List<Ride> rides = Arrays.asList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0),
            getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 30, "VashonIsland-Fauntelroy"),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 60),
            getRide(SKAGIT_TRANSIT_AGENCY_ID, 90),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 120),
            getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 150, "Fauntleroy-VashonIsland")
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 4 + 610f);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3 + 50f + 305f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 100f + DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 610f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS * 3 + 610f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f + 50f + 100f + 305f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Total trip time is 4h 30m. This is equivalent to three transfer windows and therefore three Orca fare charges.
     */
    @Test
    public void calculateFareThatExceedsTwoHourFreeTransferWindowTwice() {
        List<Ride> rides = Arrays.asList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 30),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 60),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 90),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 120),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 150),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 180),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 210),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 240),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 270)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 10);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 10);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 100f + 0f + 0f + 0f + 100f + 0f + 0f + 0f + 100f + 0f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f +
            DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f + 0f + 0f + 0f + 100f + 0f + 0f + 0f + 100f + 0f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * This trip starts with a cash fare so the free transfer window doesn't start until the second transfer. Therefore,
     * all subsequent transfers will come under one transfer window and only one Orca discount charge will apply.
     */
    @Test
    public void calculateFareThatStartsWithACashFare() {
        List<Ride> rides = Arrays.asList(
            getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 30),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 60),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 90),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 120),
            getRide(KITSAP_TRANSIT_AGENCY_ID, 149)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS +
            DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicSenior, DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Single trip with Kitsap transit fast ferry east to confirm correct non Orca fares are applied.
     */
    @Test
    public void calculateFareForKitsapFastFerryEastAgency() {
        List<Ride> rides = Collections.singletonList(
            getRide(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "Kitsap Fast Ferry", "east")
        );
        calculateFare(rides, Fare.FareType.regular, 200f);
        calculateFare(rides, Fare.FareType.senior, 200f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicRegular, 200f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Single trip (Point Defiance - Tahlequah) with WSF transit to confirm correct non Orca fares are applied.
     */
    @Test
    public void calculateFareForWSFPtToTahlequah() {
        List<Ride> rides = Collections.singletonList(
            getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0, "Point Defiance - Tahlequah")
        );
        calculateFare(rides, Fare.FareType.regular, 610f);
        calculateFare(rides, Fare.FareType.senior, 305f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 610f);
        calculateFare(rides, Fare.FareType.electronicRegular, 610f);
        calculateFare(rides, Fare.FareType.electronicSenior, 305f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Single trip with Link Light Rail to ensure distance fare is calculated correctly.
     */
    @Test
    public void calculateFareForLightRailRide() {
        List<Ride> rides = Collections.singletonList(
            getRide(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Roosevelt Station", "Int'l Dist/Chinatown")
        );
        calculateFare(rides, Fare.FareType.regular, 250f);
        calculateFare(rides, Fare.FareType.senior, 100f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 150f);
        calculateFare(rides, Fare.FareType.electronicRegular, 250f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
        // Ensure that it works in reverse
        rides = Collections.singletonList(
            getRide(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station")
        );
        calculateFare(rides, Fare.FareType.regular, 250f);
        calculateFare(rides, Fare.FareType.senior, 100f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 150f);
        calculateFare(rides, Fare.FareType.electronicRegular, 250f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    @Test
    public void calculateFareForSounderRide() {
        List<Ride> rides = Collections.singletonList(
            getRide(SOUND_TRANSIT_AGENCY_ID, "S Line", 0, "King Street Station", "Auburn Station")
        );
        calculateFare(rides, Fare.FareType.regular, 425f);
        calculateFare(rides, Fare.FareType.senior, 100f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 150f);
        calculateFare(rides, Fare.FareType.electronicRegular, 425f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
        // Ensure that it works in reverse
        rides = Collections.singletonList(
            getRide(SOUND_TRANSIT_AGENCY_ID, "N Line", 0, "King Street Station", "Everett Station")
        );
        calculateFare(rides, Fare.FareType.regular, 500f);
        calculateFare(rides, Fare.FareType.senior, 100f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 150f);
        calculateFare(rides, Fare.FareType.electronicRegular, 500f);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    /**
     * Create a few Sound Transit trips but under the contracted agency IDs.
     * SoundTransit contracts their bus service, so their routes show under the contracted agency's IDs in the GTFS feed.
     * Make sure that we get ST's bus fare and not the contracted agency's fare.
     */
    @Test
    public void calculateSoundTransitBusFares() {
        List<Ride> rides = Arrays.asList(
            getRide(COMM_TRANS_AGENCY_ID, "512", 0),
            getRide(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "594", 120),
            getRide(KC_METRO_AGENCY_ID, "550", 240)
        );
        calculateFare(rides, Fare.FareType.regular, 975f);
        calculateFare(rides, Fare.FareType.senior, 300f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 450f);
        calculateFare(rides, Fare.FareType.electronicRegular, 975f);
        calculateFare(rides, Fare.FareType.electronicSenior, 300f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);

        // Also make sure that PT's 500 and 501 get regular Pierce fare and not ST's fare
        rides = Arrays.asList(
            getRide(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "500", 0),
            getRide(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "501", 60)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    @Test
    public void calculateCashFreeTransferKCMetro() {
        List<Ride> rides = Arrays.asList(
            getRide(KC_METRO_AGENCY_ID, 0),
            getRide(KC_METRO_AGENCY_ID, 20),
            getRide(COMM_TRANS_AGENCY_ID, 45),
            getRide(KC_METRO_AGENCY_ID, 60),
            getRide(KC_METRO_AGENCY_ID, 130)
        );
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
        calculateFare(rides, Fare.FareType.senior, 325f);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 300f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
        calculateFare(rides, Fare.FareType.electronicSenior, 125f); // Transfer extended by CT ride
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);
    }

    @Test
    public void calculateTransferExtension() {
        List<Ride> rides = Arrays.asList(
            getRide(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station"),// 2.50
            getRide(SOUND_TRANSIT_AGENCY_ID, "1-Line", 60, "Roosevelt Station", "Angle Lake Station"), // 3.25, should extend transfer
            getRide(SOUND_TRANSIT_AGENCY_ID, "1-Line", 140, "Int'l Dist/Chinatown", "Angle Lake Station") // 3.00, should be free under extended transfer
        );
        calculateFare(rides, Fare.FareType.regular, 250f+325f+300f);
        calculateFare(rides, Fare.FareType.senior, 100f*3);
        calculateFare(rides, Fare.FareType.youth, 0f);
        calculateFare(rides, Fare.FareType.electronicSpecial, 150f*2);
        calculateFare(rides, Fare.FareType.electronicRegular, 325f); // transfer extended on second leg
        calculateFare(rides, Fare.FareType.electronicSenior, 100f*2);
        calculateFare(rides, Fare.FareType.electronicYouth, 0f);

    }


    private static Ride getRide(String agencyId, long startTimeMins) {
        return createRide(agencyId, "-1", -1, null, startTimeMins, "", "", "");
    }

    private static Ride getRide(String agencyId, long startTimeMins, String routeLongName) {
        return createRide(agencyId, "-1", -1, null, startTimeMins, "", "", routeLongName);
    }

    private static Ride getRide(String agencyId, long startTimeMins, int rideType, String routeId, String tripId) {
        return createRide(agencyId, "-1", rideType, null, startTimeMins, routeId, tripId, "");
    }

    private static Ride getRide(String agencyId, String shortName, long startTimeMins) {
        return createRide(agencyId, shortName, -1, null,startTimeMins, "", "", "");
    }

    private static Ride getRide(String agencyId, String shortName, long startTimeMins, String firstStopName, String lastStopName) {
        return createRide(agencyId, shortName, -1, null,startTimeMins, "", "", "", firstStopName, lastStopName);
    }

    /**
     * Create a {@link Ride} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
     * correct ride type.
     */
    private static Ride createRide(String agencyId,
                                   String shortName,
                                   int rideType,
                                   String desc,
                                   long startTimeMins,
                                   String routeId,
                                   String tripId,
                                   String routeLongName
    ) {
        return createRide(agencyId, shortName, rideType, desc, startTimeMins, routeId, tripId, routeLongName, "", "");
    }

    /**
     * Create a {@link Ride} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
     * correct ride type.
     */
    private static Ride createRide(String agencyId,
                                String shortName,
                                int rideType,
                                String desc,
                                long startTimeMins,
                                String routeId,
                                String tripId,
                                String routeLongName,
                                String firstStopName,
                                String lastStopName
    ) {
        Ride ride = new Ride();
        Agency agency = new Agency();
        agency.setId(agencyId);
        Route route = new Route();

        // Set up stops
        Stop firstStop = new Stop();
        firstStop.setName(firstStopName);
        Stop lastStop = new Stop();
        lastStop.setName(lastStopName);
        ride.firstStop = firstStop;
        ride.lastStop = lastStop;

        FeedScopedId routeFeedScopeId = new FeedScopedId();
        routeFeedScopeId.setId(routeId);
        route.setId(routeFeedScopeId);
        route.setAgency(agency);
        route.setShortName(shortName);
        route.setType(rideType);
        route.setDesc(desc);
        route.setLongName(routeLongName);
        ride.routeData = route;
        ride.startTime = startTimeMins * 60;
        FeedScopedId tripFeedScopeId = new FeedScopedId();
        tripFeedScopeId.setId(tripId);
        ride.trip = tripFeedScopeId;
        return ride;
    }
}
