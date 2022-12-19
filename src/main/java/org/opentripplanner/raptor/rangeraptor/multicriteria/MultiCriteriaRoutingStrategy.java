package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.rangeraptor.multicriteria.PatternRide.paretoComparatorRelativeCost;

import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.RoutingStrategy;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedRoutingSupport;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * The purpose of this class is to implement the multi-criteria specific functionality of the
 * worker.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class MultiCriteriaRoutingStrategy<T extends RaptorTripSchedule>
  implements RoutingStrategy<T> {

  private final McRangeRaptorWorkerState<T> state;
  private final TimeBasedRoutingSupport<T> routingSupport;
  private final ParetoSet<PatternRide<T>> patternRides;
  private final CostCalculator<T> costCalculator;
  private final SlackProvider slackProvider;

  public MultiCriteriaRoutingStrategy(
    McRangeRaptorWorkerState<T> state,
    TimeBasedRoutingSupport<T> routingSupport,
    CostCalculator<T> costCalculator,
    SlackProvider slackProvider,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    this.state = state;
    this.routingSupport = routingSupport;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.patternRides =
      new ParetoSet<>(
        paretoComparatorRelativeCost(),
        debugHandlerFactory.paretoSetPatternRideListener()
      );
  }

  @Override
  public void setAccessToStop(RaptorAccessEgress accessPath, int iterationDepartureTime) {
    int departureTime = routingSupport.getTimeDependentDepartureTime(
      accessPath,
      iterationDepartureTime
    );

    // This access is not available after the iteration departure time
    if (departureTime == -1) {
      return;
    }

    state.setAccessToStop(accessPath, departureTime);
  }

  @Override
  public void prepareForTransitWith(RaptorTimeTable<T> timeTable) {
    routingSupport.prepareForTransitWith(timeTable);
    this.patternRides.clear();
  }

  @Override
  public void alight(final int stopIndex, final int stopPos, int alightSlack) {
    for (PatternRide<T> ride : patternRides) {
      state.transitToStop(ride, stopIndex, ride.trip().arrival(stopPos), alightSlack);
    }
  }

  @Override
  public void boardWithRegularTransfer(int stopIndex, int stopPos, int boardSlack) {
    for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
      boardWithRegularTransfer(prevArrival, stopIndex, stopPos, boardSlack);
    }
  }

  @Override
  public void boardWithConstrainedTransfer(
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedTripScheduleBoardingSearch<T> txSearch
  ) {
    for (AbstractStopArrival<T> prevArrival : state.listStopArrivalsPreviousRound(stopIndex)) {
      boardWithConstrainedTransfer(prevArrival, stopIndex, stopPos, boardSlack, txSearch);
    }
  }

  private void board(
    AbstractStopArrival<T> prevArrival,
    final int stopIndex,
    final RaptorTripScheduleBoardOrAlightEvent<T> boarding
  ) {
    final T trip = boarding.getTrip();
    final int boardTime = boarding.getTime();

    if (prevArrival.arrivedByAccess()) {
      int latestArrivalTime = boardTime - slackProvider.boardSlack(trip.pattern().slackIndex());
      prevArrival = prevArrival.timeShiftNewArrivalTime(latestArrivalTime);
    }

    final int boardCost = calculateCostAtBoardTime(prevArrival, boarding);

    final int relativeBoardCost = boardCost + calculateOnTripRelativeCost(boardTime, trip);

    patternRides.add(
      new PatternRide<>(
        prevArrival,
        stopIndex,
        boarding.getStopPositionInPattern(),
        boardTime,
        boardCost,
        relativeBoardCost,
        trip
      )
    );
  }

  private void boardWithRegularTransfer(
    AbstractStopArrival<T> prevArrival,
    int stopIndex,
    int stopPos,
    int boardSlack
  ) {
    var result = routingSupport.boardWithRegularTransfer(
      prevArrival.arrivalTime(),
      stopPos,
      boardSlack
    );
    if (!result.empty()) {
      board(prevArrival, stopIndex, result);
    }
  }

  private void boardWithConstrainedTransfer(
    AbstractStopArrival<T> prevArrival,
    int stopIndex,
    int stopPos,
    int boardSlack,
    RaptorConstrainedTripScheduleBoardingSearch<T> txSearch
  ) {
    var previousTransitArrival = prevArrival.mostRecentTransitArrival();

    var boarding = routingSupport.boardWithConstrainedTransfer(
      previousTransitArrival,
      prevArrival.arrivalTime(),
      boardSlack,
      txSearch
    );
    if (boarding.empty()) {
      boardWithRegularTransfer(prevArrival, stopIndex, stopPos, boardSlack);
    } else if (!boarding.getTransferConstraint().isNotAllowed()) {
      board(prevArrival, stopIndex, boarding);
    }
  }

  /**
   * Calculate a cost for riding a trip. It should include the cost from the beginning of the
   * journey all the way until a trip is boarded. Any slack at the end of the last leg is not part
   * of this, because that is already accounted for. If the previous leg is an access leg, then it
   * is already time-shifted, which is important for this calculation to be correct.
   * <p>
   * Note! This depends on the {@code prevArrival} being set.
   */
  private int calculateCostAtBoardTime(
    AbstractStopArrival<T> prevArrival,
    final RaptorTripScheduleBoardOrAlightEvent<T> boardEvent
  ) {
    return (
      prevArrival.cost() +
      costCalculator.boardingCost(
        prevArrival.isFirstRound(),
        prevArrival.arrivalTime(),
        boardEvent.getBoardStopIndex(),
        boardEvent.getTime(),
        boardEvent.getTrip(),
        boardEvent.getTransferConstraint()
      )
    );
  }

  /**
   * Calculate a cost for riding a trip. It should include the cost from the beginning of the
   * journey all the way until a trip is boarded. The cost is used to compare trips boarding the
   * same pattern with the same number of transfers. It is ok for the cost to be relative to any
   * point in place or time - as long as it can be used to compare to paths that started at the
   * origin in the same iteration, having used the same number-of-rounds to board the same trip.
   */
  private int calculateOnTripRelativeCost(int boardTime, T tripSchedule) {
    return costCalculator.onTripRelativeRidingCost(boardTime, tripSchedule);
  }
}
