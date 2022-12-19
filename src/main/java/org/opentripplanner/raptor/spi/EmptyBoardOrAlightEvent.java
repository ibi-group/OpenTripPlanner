package org.opentripplanner.raptor.spi;

import java.util.function.Consumer;
import javax.annotation.Nonnull;

record EmptyBoardOrAlightEvent<T extends RaptorTripSchedule>(int earliestBoardTime)
  implements RaptorTripScheduleBoardOrAlightEvent<T> {
  @Override
  public int getTripIndex() {
    return NOT_FOUND;
  }

  @Override
  public T getTrip() {
    return null;
  }

  @Override
  public int getStopPositionInPattern() {
    return NOT_FOUND;
  }

  @Override
  public int getTime() {
    return NOT_FOUND;
  }

  @Override
  public int getEarliestBoardTime() {
    return earliestBoardTime;
  }

  @Nonnull
  @Override
  public RaptorTransferConstraint getTransferConstraint() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  @Override
  public boolean empty() {
    return true;
  }

  @Override
  public void boardWithFallback(
    Consumer<RaptorTripScheduleBoardOrAlightEvent<T>> boardCallback,
    Consumer<RaptorTripScheduleBoardOrAlightEvent<T>> fallback
  ) {
    fallback.accept(this);
  }
}
