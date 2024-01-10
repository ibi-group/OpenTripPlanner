package org.opentripplanner.routing.alertpatch;

/**
 * Represents a period of time, in terms of seconds in [start, end)
 */
public class TimePeriod {

  public static final long OPEN_ENDED = Long.MAX_VALUE;

  public final long startTime;

  public final long endTime;

  public TimePeriod(long start, long end) {
    this.startTime = start;
    this.endTime = end;
  }

  public int hashCode() {
    return (int) ((startTime & 0x7fff) + (endTime & 0x7fff));
  }

  public boolean equals(Object o) {
    if (!(o instanceof TimePeriod other)) {
      return false;
    }
    return other.startTime == startTime && other.endTime == endTime;
  }
}
