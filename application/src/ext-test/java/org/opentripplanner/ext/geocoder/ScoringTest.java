package org.opentripplanner.ext.geocoder;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

public class ScoringTest {

  private static final WgsCoordinate FOCUS_POINT = new WgsCoordinate(1.10001, 1.10001);
  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private final RegularStop stop1 = testModel
    .stop("1")
    .withCoordinate(1, 1)
    .withCode("1111")
    .build();
  private final RegularStop stop2 = testModel
    .stop("2")
    .withCoordinate(1.1, 1.1)
    .withCode("1111")
    .build();
  private final RegularStop stop3 = testModel
    .stop("3")
    .withCoordinate(1.11, 1.11)
    .withCode("1111")
    .build();
  private final RegularStop stop4 = testModel
    .stop("4")
    .withCoordinate(1.2, 1.2)
    .withName(I18NString.of("1111"))
    .build();

  @Test
  void codeMatch() {
    var index = buildIndex(stop1, stop2, stop3);
    var result = index.queryStopClusters("1111", null).map(c -> c.primary().id());
    assertThat(result).containsExactlyElementsIn(toIds(stop1, stop3, stop2)).inOrder();
  }

  @Test
  void codeMatchWithFocusPoint() {
    var index = buildIndex(stop1, stop2, stop3);
    var result = index.queryStopClusters("1111", FOCUS_POINT).map(c -> c.primary().id());
    assertThat(result).containsExactlyElementsIn(toIds(stop2, stop3, stop1)).inOrder();
  }

  @Test
  void nameToBottom() {
    var index = buildIndex(stop1, stop2, stop3, stop4);
    var result = index.queryStopClusters("1111", null).map(c -> c.primary().id());
    assertThat(result).containsExactlyElementsIn(toIds(stop1, stop3, stop2, stop4)).inOrder();
  }

  @Test
  void codeMatchesSortedByDistance() {
    var index = buildIndex(stop1, stop2, stop3, stop4);
    var result = index.queryStopClusters("1111", FOCUS_POINT).map(c -> c.primary().id());
    assertThat(result).containsExactlyElementsIn(toIds(stop2, stop3, stop1, stop4)).inOrder();
  }

  private LuceneIndex buildIndex(RegularStop... stops) {
    var siteRepository = testModel.siteRepositoryBuilder();
    Arrays.stream(stops).forEach(siteRepository::withRegularStop);
    var timetableRepository = new TimetableRepository(siteRepository.build(), new Deduplicator());
    timetableRepository.index();
    var transitService = new DefaultTransitService(timetableRepository);
    var stopConsolidationService = new DefaultStopConsolidationService(
      new DefaultStopConsolidationRepository(),
      timetableRepository
    );
    return new LuceneIndex(transitService, stopConsolidationService);
  }

  private static List<FeedScopedId> toIds(RegularStop... stops) {
    return Arrays.stream(stops).map(RegularStop::getId).toList();
  }
}
