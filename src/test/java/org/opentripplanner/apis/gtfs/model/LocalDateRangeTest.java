package org.opentripplanner.apis.gtfs.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDateRangeTest {

  private static final LocalDate DATE = LocalDate.parse("2024-06-01");

  @Test
  void unlimited() {
    assertFalse(new LocalDateRange(DATE, DATE).unlimited());
  }

  @Test
  void limited() {
    assertFalse(new LocalDateRange(DATE, null).unlimited());
    assertFalse(new LocalDateRange(null, DATE).unlimited());
  }
}
