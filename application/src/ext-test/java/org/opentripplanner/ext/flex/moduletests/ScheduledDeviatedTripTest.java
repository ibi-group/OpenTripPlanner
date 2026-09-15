package org.opentripplanner.ext.flex.moduletests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.TestFlexRouter;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;

class ScheduledDeviatedTripTest {

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of();
  private final RegularStop r1 = builder.stop("stop1");
  private final RegularStop r2 = builder.stop("stop2");
  private final AreaStop a1 = builder.areaStop("area1");
  private final AreaStop a2 = builder.areaStop("area2");
  private final TripInput trip = TripInput.flex("ft1")
    .addStop(r1, "10:00")
    .addStop(a1, "10:00", "11:00")
    .addStop(a2, "11:00", "12:00")
    .addStop(r2, "12:01");

  @Test
  void firstHop() {
    OTPFeature.FlexRouting.testOn(() -> {
      var env = builder.addTrip(trip).build();
      var summary = TestFlexRouter.of(env).withStart(r1).withEnd(a1).routeDirect("10:00");
      assertThat(summary.summarize()).containsExactly("F:ft1 10:00[F:stop1] → 11:00[F:area1]");
    });
  }

  @Test
  void fullTrip() {
    OTPFeature.FlexRouting.testOn(() -> {
      var env = builder.addTrip(trip).build();
      var summary = TestFlexRouter.of(env).withStart(r1).withEnd(a2).routeDirect("10:00");
      assertThat(summary.summarize()).containsExactly("F:ft1 10:00[F:stop1] → 12:00[F:area2]");
    });
  }

  @Test
  void fixedToFixed() {
    OTPFeature.FlexRouting.testOn(() -> {
      var env = builder.addTrip(trip).build();
      var summary = TestFlexRouter.of(env).withStart(r1).withEnd(r2).routeDirect("10:00");
      assertThat(summary.summarize()).containsExactly("F:ft1 10:00[F:stop1] → 12:01[F:stop2]");
    });
  }

  @Test
  void tooLate() {
    OTPFeature.FlexRouting.testOn(() -> {
      var env = builder.addTrip(trip).build();
      var summary = TestFlexRouter.of(env).withStart(r1).withEnd(a1).routeDirect("10:01");
      assertThat(summary.summarize()).isEmpty();
    });
  }

  @Test
  void earlier() {
    OTPFeature.FlexRouting.testOn(() -> {
      var env = builder.addTrip(trip).build();
      var summary = TestFlexRouter.of(env).withStart(r1).withEnd(a1).routeDirect("09:55");
      assertThat(summary.summarize()).containsExactly("F:ft1 10:00[F:stop1] → 11:00[F:area1]");
    });
  }
}
