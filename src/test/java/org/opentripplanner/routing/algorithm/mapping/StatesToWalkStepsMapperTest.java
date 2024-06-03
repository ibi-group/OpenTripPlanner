package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.RelativeDirection.ENTER_STATION;
import static org.opentripplanner.model.plan.RelativeDirection.EXIT_STATION;
import static org.opentripplanner.model.plan.RelativeDirection.FOLLOW_SIGNS;
import static org.opentripplanner.routing.algorithm.mapping.StatesToWalkStepsMapper.isOnSameStreet;

import com.beust.jcommander.internal.Lists;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.model.plan.WalkStepBuilder;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StatesToWalkStepsMapperTest {

  @Test
  void absoluteDirection() {
    var walkSteps = buildWalkSteps(TestStateBuilder.ofWalking().streetEdge().streetEdge());
    assertEquals(2, walkSteps.size());
    walkSteps.forEach(step -> assertTrue(step.getAbsoluteDirection().isPresent()));
  }

  @Test
  void elevator() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge()
    );
    var elevatorStep = walkSteps.get(3);
    assertEquals(RelativeDirection.ELEVATOR, elevatorStep.getRelativeDirection());
    assertTrue(elevatorStep.getAbsoluteDirection().isEmpty());
  }

  @Test
  void enterStation() {
    final TestStateBuilder builder = TestStateBuilder
      .ofWalking()
      .streetEdge()
      .enterStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var enter = walkSteps.get(1);
    assertEquals(enter.getRelativeDirection(), ENTER_STATION);
  }

  @Test
  void exitStation() {
    final TestStateBuilder builder = TestStateBuilder
      .ofWalking()
      .streetEdge()
      .exitStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(3, walkSteps.size());
    var enter = walkSteps.get(2);
    assertEquals(enter.getRelativeDirection(), EXIT_STATION);
  }

  @Test
  void signpostedPathway() {
    final String sign = "follow signs to platform 1";
    final TestStateBuilder builder = TestStateBuilder.ofWalking().streetEdge().pathway(sign);
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var step = walkSteps.get(1);
    assertEquals(step.getRelativeDirection(), FOLLOW_SIGNS);
    assertEquals(sign, step.getDirectionText().toString());
  }

  private static List<WalkStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new GraphPath<>(result);
    var mapper = new StatesToWalkStepsMapper(path.states, null, new StreetNotesService(), 0);
    return mapper.generateWalkSteps();
  }

  @ParameterizedTest
  @MethodSource("createIsOnSameStreetCases")
  void testIsOnSameStreet(List<String> streets, boolean expected, String message) {
    List<WalkStepBuilder> steps = streets
      .stream()
      .map(s ->
        s != null ? WalkStep.builder().withDirectionText(I18NString.of(s)) : WalkStep.builder()
      )
      .toList();

    int lastIndex = steps.size() - 1;
    WalkStepBuilder threeBack = steps.get(lastIndex - 2);
    WalkStepBuilder twoBack = steps.get(lastIndex - 1);
    WalkStepBuilder lastStep = steps.get(lastIndex);

    assertEquals(expected, isOnSameStreet(lastStep, twoBack, threeBack), message);
  }

  static Stream<Arguments> createIsOnSameStreetCases() {
    return Stream.of(
      Arguments.of(List.of("Street1", "Street2", "Street3"), false, "Is not a zig-zag"),
      Arguments.of(List.of("Street1", "Street2", "Street1"), true, "Is a zig-zag"),
      Arguments.of(List.of("Street1", "crossing over Street2", "Street1"), false, "Is a crossing"),
      Arguments.of(
        List.of("crossing over turn lane", "Street1", "crossing over turn lane"),
        false,
        "Is many crossings"
      ),
      Arguments.of(Lists.newArrayList(null, null, null), false, "Is not a zig-zag")
    );
  }
}
