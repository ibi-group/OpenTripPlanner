package org.opentripplanner.api.model;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Represents either an earliest or latest time a trip can be booked relative to the departure day
 * of the trip.
 */
public class ApiBookingTime implements Serializable {
  public final int time;

  public final int daysPrior;

  public ApiBookingTime(int time, int daysPrior) {
    this.time = time;
    this.daysPrior = daysPrior;
  }

}
