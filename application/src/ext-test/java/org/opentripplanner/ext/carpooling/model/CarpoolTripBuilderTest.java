package org.opentripplanner.ext.carpooling.model;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

public class CarpoolTripBuilderTest {

  @Test
  void buildFromValues_fromId_buildToCorrectValues() {
    var startTime = ZonedDateTime.now();
    var endTime = ZonedDateTime.now().plusMinutes(45);
    var stop = createStopAt(OSLO_EAST);

    var builder = new CarpoolTripBuilder(new FeedScopedId("feed", "id"));
    var trip = builder
      .withTotalCapacity(2)
      .withProvider("UNIT")
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withStops(List.of(stop))
      .buildFromValues();

    assertEquals(2, trip.totalCapacity());
    assertEquals("UNIT", trip.provider());
    assertEquals(startTime, trip.startTime());
    assertEquals(endTime, trip.endTime());
    assertEquals(stop, trip.stops().getFirst());
  }

  @Test
  void buildFromValues_fromOriginal_buildToCorrectValues() {
    var original = createSimpleTrip(OSLO_EAST, OSLO_NORTH);

    var builder = new CarpoolTripBuilder(original);
    var trip = builder.buildFromValues();

    assertEquals(original.totalCapacity(), trip.totalCapacity());
    assertEquals(original.provider(), trip.provider());
    assertEquals(original.startTime(), trip.startTime());
    assertEquals(original.endTime(), trip.endTime());
    assertEquals(original.stops().getFirst(), trip.stops().getFirst());
    assertEquals(original.stops().getLast(), trip.stops().getLast());
  }
}
