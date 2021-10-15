package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Modifying a {@link TransitPathLeg} requires recalculating. This builder help cashing
 * new fields and calculating the cost for the caller.
 */
public class TransitPathLegBuilder<T extends RaptorTripSchedule> {

  public static final int NOT_SET = -999_999;
  private final TransitPathLeg<T> original;

  private int newBoardStop = NOT_SET;
  private int newBoardTime = NOT_SET;
  private int newAlightTime = NOT_SET;
  private PathLeg<T> next = null;

  TransitPathLegBuilder(TransitPathLeg<T> original) {
    this.original = original;
  }

  public int boardStop() {
    return newBoardStop == NOT_SET ? original.fromStop() : newBoardStop;
  }

  public int boardTime() {
    return newBoardTime == NOT_SET ? original.fromTime() : newBoardTime;
  }

  public int alightStop() {
    return next == null ? original.toStop() : next.fromStop();
  }

  public int alightTime() {
    return newAlightTime == NOT_SET ? original.toTime() : newAlightTime;
  }

  public PathLeg<T> next() {
    return next == null ? original.nextLeg() : next;
  }

  public T trip() {
    return original.trip();
  }

  public TransitPathLegBuilder<T> boardStop(int boardStop, int boardTime) {
    this.newBoardStop = boardStop;
    this.newBoardTime = boardTime;
    return this;
  }

  public TransitPathLegBuilder<T> newTail(int alightTime, PathLeg<T> next) {
    this.newAlightTime = alightTime;
    this.next = next;
    return this;
  }

  public TransitPathLeg<T> build(
      CostCalculator costCalculator,
      RaptorSlackProvider slackProvider,
      boolean firstTransit,
      int fromStopArrivalTime
  ) {
    int cost = cost(costCalculator, slackProvider, firstTransit, fromStopArrivalTime);
    return new TransitPathLeg<>(
        boardStop(), boardTime(), alightStop(), alightTime(), cost, trip(), next()
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransitPathLegBuilder.class)
        .addNum("newBoardStop", newBoardStop, NOT_SET)
        .addServiceTime("newBoardTime", newBoardTime, NOT_SET)
        .addServiceTime("newAlightTime", newAlightTime, NOT_SET)
        .addObj("next", next)
        .addObj("original", original)
        .toString();
  }


  /* private methods */

  private int cost(
      CostCalculator calc,
      RaptorSlackProvider slackProvider,
      boolean firstTransit,
      int fromStopArrivalTime
  ) {
    int alightSlack = slackProvider.alightSlack(trip().pattern());
    int waitTimeBoarding = boardTime() - fromStopArrivalTime;
    int transitTime =  alightTime() - boardTime();
    int boardCost = calc.boardCost(firstTransit, waitTimeBoarding, boardStop());

    return calc.transitArrivalCost(
            boardCost,
            alightSlack,
            transitTime,
            trip().transitReluctanceFactorIndex(),
            alightStop()
    );
  }
}
