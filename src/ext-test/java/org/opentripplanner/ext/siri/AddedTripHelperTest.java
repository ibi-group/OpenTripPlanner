package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class AddedTripHelperTest {

  private Agency agency;
  private Agency externalAgency;
  private Operator operator;
  private Trip trip;
  private FeedScopedId routeId;
  private Mode transitMode;
  private NaturalLanguageStringStructure publishedNames;

  @BeforeEach
  public void setUp() {
    agency =
      Agency
        .of(new FeedScopedId("FEED_ID", "AGENCY_ID"))
        .withName("AGENCY_NAME")
        .withTimezone("CET")
        .build();

    operator =
      Operator.of(new FeedScopedId("FEED_ID", "OPERATOR_ID")).withName("OPERATOR_NAME").build();

    var externalId = new FeedScopedId("EXTERNAL", "NOT TO BE USED");
    externalAgency =
      Agency
        .of(externalId)
        .withName("EXTERNAL UNKNOWN AGENCY")
        .withTimezone("Europe/Berlin")
        .build();

    routeId = new FeedScopedId("FEED_ID", "ROUTE_ID");
    transitMode = new Mode(TransitMode.RAIL, "replacementRailService");
    publishedNames = new NaturalLanguageStringStructure();
    publishedNames.setLang("en");
    publishedNames.setValue("Hogwarts Express");

    var headsign = new NonLocalizedString("TEST TRIP TOWARDS TEST ISLAND");
    trip =
      Trip
        .of(new FeedScopedId("FEED_ID", "TEST_TRIP"))
        .withRoute(getRouteWithAgency(agency, operator))
        .withHeadsign(headsign)
        .build();
  }

  @Test
  public void testGetTrip_FailOnMissingServiceId() {
    var actualTrip = AddedTripHelper.getTrip(null, null, null, null, null, null);

    assertAll(() -> {
      assertTrue(actualTrip.isFailure(), "Trip creation should fail");
      assertEquals(
        UpdateError.UpdateErrorType.NO_START_DATE,
        actualTrip.failureValue().errorType(),
        "Trip creation should fail without start date"
      );
    });
  }

  @Test
  public void testGetTrip() {
    var route = getRouteWithAgency(agency, operator);
    var destinationName = new NaturalLanguageStringStructure();
    var transitMode = new Mode(TransitMode.RAIL, "replacementRailService");
    var serviceId = new FeedScopedId("FEED ID", "CS ID");

    var actualTrip = AddedTripHelper.getTrip(
      trip.getId(),
      route,
      operator,
      transitMode,
      List.of(destinationName),
      serviceId
    );

    assertAll(() -> {
      assertTrue(actualTrip.isSuccess(), "Trip creation should succeed");
      assertEquals(trip.getId(), actualTrip.successValue().getId(), "Trip is should be mapped");
      assertEquals(
        operator,
        actualTrip.successValue().getOperator(),
        "operator is should be mapped"
      );
      assertEquals(route, actualTrip.successValue().getRoute(), "route is should be mapped");
      assertEquals(
        transitMode.mode(),
        actualTrip.successValue().getMode(),
        "transitMode is should be mapped"
      );
      assertEquals(
        SubMode.of(transitMode.submode()),
        actualTrip.successValue().getNetexSubMode(),
        "submode is should be mapped"
      );
      assertEquals(
        serviceId,
        actualTrip.successValue().getServiceId(),
        "serviceId is should be mapped"
      );
    });
  }

  @Test
  public void testGetRouteEmptyName() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, operator);
    var replacedRoute = getRouteWithAgency(externalAgency, operator);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(),
      operator,
      replacedRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertNotNull(actualRoute.getName(), "Name should be empty string not null");
      assertNotEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(agency, actualRoute.getAgency(), "Agency should be taken from replaced route");
    });
  }

  @Test
  public void testGetRouteWithAgencyFromReplacedRoute() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, null);
    var replacedRoute = getRouteWithAgency(externalAgency, operator);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(publishedNames),
      operator,
      replacedRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(
        externalAgency,
        actualRoute.getAgency(),
        "Agency should be taken from replaced route"
      );
    });
  }

  @Test
  public void testGetRoute() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, operator);
    var externalRoute = getRouteWithAgency(externalAgency, null);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(publishedNames),
      operator,
      externalRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(agency, actualRoute.getAgency(), "Agency should be taken from operator");
      assertNotEquals(
        externalAgency,
        actualRoute.getAgency(),
        "External agency should not be used"
      );
    });
  }

  private Route getRouteWithAgency(Agency agency, Operator operator) {
    return Route
      .of(new FeedScopedId("FEED_ID", "LINE_ID"))
      .withShortName("LINE_SHORT_NAME")
      .withLongName(new NonLocalizedString("LINE_LONG_NAME"))
      .withMode(TransitMode.RAIL)
      .withAgency(agency)
      .withOperator(operator)
      .build();
  }

  @ParameterizedTest
  @CsvSource(
    {
      "air,AIRPLANE,AIRPLANE,",
      "bus,BUS,RAIL,railReplacementBus",
      "rail,RAIL,RAIL,replacementRailService",
    }
  )
  public void testGetTransportMode(
    String siriMode,
    String internalMode,
    String replacedRouteMode,
    String subMode
  ) {
    // Arrange
    var route = Route
      .of(new FeedScopedId("FEED_ID", "LINE_ID"))
      .withShortName("LINE_SHORT_NAME")
      .withAgency(agency)
      .withMode(TransitMode.valueOf(replacedRouteMode))
      .build();
    var modes = List.of(VehicleModesEnumeration.fromValue(siriMode));

    // Act
    var mode = AddedTripHelper.getTransitMode(modes, route);

    //Assert
    var expectedMode = TransitMode.valueOf(internalMode);
    assertNotNull(mode, "TransitMode response should never be null");
    assertEquals(expectedMode, mode.mode(), "Mode not mapped to correct internal mode");
    assertEquals(subMode, mode.submode(), "Mode not mapped to correct sub mode");
  }

  @ParameterizedTest
  @CsvSource({ "10,11,0,3,true", "10,11,2,3,true", "10,11,1,3,false" })
  public void testGetTimeForStop(
    int arrivalTime,
    int departureTime,
    int stopIndex,
    int numStops,
    boolean expectedEqual
  ) {
    var arrivalAndDepartureTime = AddedTripHelper.getTimeForStop(
      arrivalTime,
      departureTime,
      stopIndex,
      numStops
    );

    if (expectedEqual) {
      assertEquals(
        arrivalAndDepartureTime.arrivalTime(),
        arrivalAndDepartureTime.departureTime(),
        "Arrival and departure time are expected to be equal"
      );
    } else {
      assertNotEquals(
        arrivalAndDepartureTime.arrivalTime(),
        arrivalAndDepartureTime.departureTime(),
        "Arrival and departure time are expected to differ"
      );
    }
  }

  @Test
  public void testCreateStopTime() {
    var actualStopSequence = 123;

    var regularStop = RegularStop.of(new FeedScopedId("FEED_ID", "TEST_STOP")).build();
    var stopTime = AddedTripHelper.createStopTime(
      trip,
      actualStopSequence,
      regularStop,
      1000,
      1200,
      ""
    );

    assertAll(() -> {
      assertEquals(trip, stopTime.getTrip(), "Trip not mapped correctly");
      assertEquals(regularStop, stopTime.getStop(), "Stop not mapped correctly");
      assertEquals(
        actualStopSequence,
        stopTime.getStopSequence(),
        "StopSequence not mapped correctly"
      );
      assertEquals(
        actualStopSequence,
        stopTime.getStopSequence(),
        "StopSequence not mapped correctly"
      );
      assertEquals(1000, stopTime.getArrivalTime(), "ArrivalTime not mapped correctly");
      assertEquals(1200, stopTime.getDepartureTime(), "DepartureTime not mapped correctly");
      assertEquals(
        trip.getHeadsign(),
        stopTime.getStopHeadsign(),
        "DepartureTime not mapped correctly"
      );
    });
  }

  @Test
  public void testCreateStopTimeOverrideHeadsignOnStop() {
    var actualStopSequence = 123;

    var regularStop = RegularStop.of(new FeedScopedId("FEED_ID", "TEST_STOP")).build();
    String stop_headsign = "STOP_HEADSIGN";
    var stopTime = AddedTripHelper.createStopTime(
      trip,
      actualStopSequence,
      regularStop,
      1000,
      1200,
      stop_headsign
    );

    assertAll(() -> {
      assertEquals(trip, stopTime.getTrip(), "Trip not mapped correctly");
      assertEquals(regularStop, stopTime.getStop(), "Stop not mapped correctly");
      assertEquals(
        actualStopSequence,
        stopTime.getStopSequence(),
        "StopSequence not mapped correctly"
      );
      assertEquals(
        actualStopSequence,
        stopTime.getStopSequence(),
        "StopSequence not mapped correctly"
      );
      assertEquals(1000, stopTime.getArrivalTime(), "ArrivalTime not mapped correctly");
      assertEquals(1200, stopTime.getDepartureTime(), "DepartureTime not mapped correctly");
      assertEquals(
        stop_headsign,
        stopTime.getStopHeadsign().toString(),
        "DepartureTime not mapped correctly"
      );
    });
  }
}
