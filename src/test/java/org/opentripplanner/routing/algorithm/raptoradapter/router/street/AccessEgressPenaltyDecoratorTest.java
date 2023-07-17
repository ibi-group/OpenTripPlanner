package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class AccessEgressPenaltyDecoratorTest {

  private static final int DURATION_CAR_RENTAL = 45;
  private static final int DURATION_WALKING = 135;
  private static final Duration D10m = DurationUtils.duration("10m");
  private static final DefaultAccessEgress WALK = ofWalking(DURATION_WALKING);
  private static final DefaultAccessEgress CAR_RENTAL = ofCarRental(DURATION_CAR_RENTAL);
  private static final TimeAndCostPenalty PENALTY = new TimeAndCostPenalty(
    TimePenalty.of(D10m, 1.5),
    2.0
  );

  private static final DefaultAccessEgress WALK_W_PENALTY = WALK.withPenalty(
    PENALTY.calculate(DURATION_WALKING)
  );
  private static final DefaultAccessEgress CAR_RENTAL_W_PENALTY = CAR_RENTAL.withPenalty(
    PENALTY.calculate(DURATION_CAR_RENTAL)
  );

  private static List<Arguments> decorateCarRentalTestCase() {
    return List.of(
      Arguments.of(List.of(), List.of()),
      Arguments.of(List.of(WALK), List.of(WALK)),
      Arguments.of(List.of(CAR_RENTAL_W_PENALTY), List.of(CAR_RENTAL)),
      Arguments.of(List.of(WALK, CAR_RENTAL_W_PENALTY), List.of(WALK, CAR_RENTAL))
    );
  }

  @ParameterizedTest
  @MethodSource("decorateCarRentalTestCase")
  void decorateCarRentalTest(List<DefaultAccessEgress> expected, List<DefaultAccessEgress> input) {
    var request = createRequestWithPenaltyForMode(StreetMode.CAR_RENTAL);
    request.journey().setModes(RequestModes.of().withAccessMode(StreetMode.CAR_RENTAL).build());

    var subject = new AccessEgressPenaltyDecorator(request);

    // Only access is decorated, since egress mode is WALK
    assertEquals(expected, subject.decorateAccess(input));
    assertEquals(input, subject.decorateEgress(input));
  }

  private static List<Arguments> decorateWalkTestCase() {
    return List.of(
      Arguments.of(List.of(), List.of()),
      Arguments.of(List.of(WALK_W_PENALTY), List.of(WALK))
    );
  }

  @ParameterizedTest
  @MethodSource("decorateWalkTestCase")
  void decorateWalkTest(List<DefaultAccessEgress> expected, List<DefaultAccessEgress> input) {
    var request = createRequestWithPenaltyForMode(StreetMode.WALK);
    request.journey().setModes(RequestModes.of().withAccessMode(StreetMode.CAR_RENTAL).build());

    var subject = new AccessEgressPenaltyDecorator(request);

    // Only egress is decorated, since access mode is not WALKING (but CAR_RENTAL)
    assertEquals(expected, subject.decorateAccess(input));
    assertEquals(expected, subject.decorateEgress(input));
  }

  @Test
  void doNotDecorateAnyIfNoPenaltyIsSet() {
    // Set penalty on BIKE, should not have an effect on the decoration
    var request = createRequestWithPenaltyForMode(StreetMode.BIKE);
    request.journey().setModes(RequestModes.of().withAccessMode(StreetMode.CAR_RENTAL).build());
    var input = List.of(WALK, CAR_RENTAL);

    var subject = new AccessEgressPenaltyDecorator(request);

    assertSame(input, subject.decorateAccess(input));
    assertSame(input, subject.decorateEgress(input));
  }

  private RouteRequest createRequestWithPenaltyForMode(StreetMode mode) {
    var request = new RouteRequest();
    request.withPreferences(pref ->
      pref.withStreet(s -> s.withAccessEgressPenalty(p -> p.with(mode, PENALTY)))
    );
    return request;
  }

  @Test
  void filterEgress() {}

  private static DefaultAccessEgress ofCarRental(int duration) {
    return ofAccessEgress(
      duration,
      TestStateBuilder.ofCarRental().streetEdge().pickUpCar().build()
    );
  }

  private static DefaultAccessEgress ofWalking(int durationInSeconds) {
    return ofAccessEgress(durationInSeconds, TestStateBuilder.ofWalking().streetEdge().build());
  }

  private static DefaultAccessEgress ofAccessEgress(int duration, State state) {
    // We do NOT need to override #withPenalty(...), because all fields including
    // 'durationInSeconds' is copied over using the getters.

    return new DefaultAccessEgress(1, state) {
      @Override
      public int durationInSeconds() {
        return duration;
      }
    };
  }
}
