package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class TimetableTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Route ROUTE = TimetableRepositoryForTest.route("routeId").build();
  public static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C").build();

  @ParameterizedTest
  @CsvSource(
    value = """
    Description           | Timetable      | Expected number of days
    Same day              | 08:00 22:00    | 0
    Same day, exact limit | 08:00 23:59    | 0
    Night bus             | 22:00 1:00+1d  | 1
    Overnight exact limit | 22:59 23:59+1d | 1
    2 overnights          | 1:00 1:00+2d   | 2
    """,
    delimiter = '|',
    useHeadersInDisplayName = true
  )
  void maxTripSpanDays(String testCaseName, String schedule, int expectedNumberOfDays) {
    var timetable = TripPattern.of(id(testCaseName))
      .withRoute(ROUTE)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_A, STOP_C))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of()
            .withTrip(TimetableRepositoryForTest.trip("t1").build())
            .withDepartureTimes(schedule)
            .build()
        )
      )
      .build()
      .getScheduledTimetable();

    assertEquals(expectedNumberOfDays, timetable.getMaxTripSpanDays());
  }
}
