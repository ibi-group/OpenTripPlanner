package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.updater.spi.UpdateError.UpdateErrorType;
import org.opentripplanner.utils.lang.StringUtils;
import uk.org.siri.siri21.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OccupancyEnumeration;
import uk.org.siri.siri21.RecordedCall;

/**
 * This class is a wrapper around either a {@link RecordedCall} or an {@link EstimatedCall}, making
 * it possible to iterate over both of the types at once.
 */
public interface CallWrapper {
  static CallWrapper of(EstimatedCall estimatedCall) {
    return new EstimatedCallWrapper(estimatedCall);
  }

  static CallWrapper of(RecordedCall recordedCall) {
    return new RecordedCallWrapper(recordedCall);
  }

  static List<CallWrapper> of(EstimatedVehicleJourney estimatedVehicleJourney) {
    List<CallWrapper> result = new ArrayList<>();

    if (estimatedVehicleJourney.getRecordedCalls() != null) {
      for (var recordedCall : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
        result.add(new RecordedCallWrapper(recordedCall));
      }
    }

    if (estimatedVehicleJourney.getEstimatedCalls() != null) {
      for (var estimatedCall : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
        result.add(new EstimatedCallWrapper(estimatedCall));
      }
    }

    return List.copyOf(result);
  }

  /**
   * Validate that all calls have a non-empty stop point ref and either order or visit number (but
   * not both). Also checks cross-call consistency: all calls must use the same strategy (all Order
   * or all VisitNumber). A mix of Order and VisitNumber-only calls is rejected.
   */
  static Optional<UpdateErrorType> validateAll(List<CallWrapper> calls) {
    var perCallError = calls
      .stream()
      .map(CallWrapper::validate)
      .flatMap(Optional::stream)
      .findFirst();
    if (perCallError.isPresent()) {
      return perCallError;
    }
    boolean anyHasOrder = calls.stream().anyMatch(CallWrapper::hasOrder);
    boolean anyMissingOrder = calls.stream().anyMatch(c -> !c.hasOrder());
    if (anyHasOrder && anyMissingOrder) {
      return Optional.of(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER);
    }
    return Optional.empty();
  }

  /**
   * Validate that the call has a non-empty stop point ref and either order or visit number (but not
   * both).
   */
  default Optional<UpdateErrorType> validate() {
    if (StringUtils.hasNoValueOrNullAsString(getStopPointRef())) {
      return Optional.of(UpdateErrorType.EMPTY_STOP_POINT_REF);
    }
    if (!hasOrder() && !hasVisitNumber()) {
      return Optional.of(UpdateErrorType.MISSING_CALL_ORDER);
    }
    if (hasOrder() && hasVisitNumber()) {
      return Optional.of(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER);
    }
    return Optional.empty();
  }

  String getStopPointRef();

  boolean hasOrder();

  boolean hasVisitNumber();

  /**
   * Return the sort order of this call. Prefers Order if present, falls back to VisitNumber.
   */
  int getSortOrder();

  Boolean isCancellation();
  Boolean isPredictionInaccurate();
  boolean isExtraCall();
  OccupancyEnumeration getOccupancy();
  List<NaturalLanguageStringStructure> getDestinationDisplays();
  ZonedDateTime getAimedArrivalTime();
  ZonedDateTime getExpectedArrivalTime();
  ZonedDateTime getActualArrivalTime();
  CallStatusEnumeration getArrivalStatus();
  ArrivalBoardingActivityEnumeration getArrivalBoardingActivity();
  ZonedDateTime getAimedDepartureTime();
  ZonedDateTime getExpectedDepartureTime();
  ZonedDateTime getActualDepartureTime();
  CallStatusEnumeration getDepartureStatus();
  DepartureBoardingActivityEnumeration getDepartureBoardingActivity();

  /// Whether the call is a RecordedCall or not
  boolean isRecorded();

  /// Whether the vehicle has arrived at the stop.
  default boolean hasArrived() {
    return isRecorded() || getArrivalStatus() == CallStatusEnumeration.ARRIVED;
  }

  /// Whether the vehicle has departed from the stop.
  default boolean hasDeparted() {
    return isRecorded() && getActualDepartureTime() != null;
  }

  final class EstimatedCallWrapper implements CallWrapper {

    private final EstimatedCall call;

    private EstimatedCallWrapper(EstimatedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
    }

    @Override
    public boolean hasOrder() {
      return call.getOrder() != null;
    }

    @Override
    public boolean hasVisitNumber() {
      return call.getVisitNumber() != null;
    }

    /**
     * Return the call order, either from the Order field or the VisitNumber field.
     * Validation ensures that one of them is set.
     * See {@link #validate()}
     * @return
     */
    @Override
    public int getSortOrder() {
      return call.getOrder() != null
        ? call.getOrder().intValueExact()
        : call.getVisitNumber().intValueExact();
    }

    @Override
    public Boolean isCancellation() {
      return call.isCancellation();
    }

    @Override
    public Boolean isPredictionInaccurate() {
      return call.isPredictionInaccurate();
    }

    @Override
    public boolean isExtraCall() {
      return Boolean.TRUE.equals(call.isExtraCall());
    }

    @Override
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplays() {
      return call.getDestinationDisplaies();
    }

    @Override
    public ZonedDateTime getAimedArrivalTime() {
      return call.getAimedArrivalTime();
    }

    @Override
    public ZonedDateTime getExpectedArrivalTime() {
      return call.getExpectedArrivalTime();
    }

    @Override
    public ZonedDateTime getActualArrivalTime() {
      return null;
    }

    @Override
    public CallStatusEnumeration getArrivalStatus() {
      return call.getArrivalStatus();
    }

    @Override
    public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
      return call.getArrivalBoardingActivity();
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }

    @Override
    public ZonedDateTime getExpectedDepartureTime() {
      return call.getExpectedDepartureTime();
    }

    @Override
    public ZonedDateTime getActualDepartureTime() {
      return null;
    }

    @Override
    public CallStatusEnumeration getDepartureStatus() {
      return call.getDepartureStatus();
    }

    @Override
    public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
      return call.getDepartureBoardingActivity();
    }

    @Override
    public boolean isRecorded() {
      return false;
    }

    @Override
    public int hashCode() {
      return call.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof EstimatedCallWrapper estimatedCallWrapper)) {
        return false;
      }
      return call.equals(estimatedCallWrapper.call);
    }
  }

  final class RecordedCallWrapper implements CallWrapper {

    private final RecordedCall call;

    private RecordedCallWrapper(RecordedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
    }

    @Override
    public boolean hasOrder() {
      return call.getOrder() != null;
    }

    @Override
    public boolean hasVisitNumber() {
      return call.getVisitNumber() != null;
    }

    @Override
    public int getSortOrder() {
      return call.getOrder() != null
        ? call.getOrder().intValueExact()
        : call.getVisitNumber().intValueExact();
    }

    @Override
    public Boolean isCancellation() {
      return call.isCancellation();
    }

    @Override
    public Boolean isPredictionInaccurate() {
      return call.isPredictionInaccurate();
    }

    @Override
    public boolean isExtraCall() {
      return Boolean.TRUE.equals(call.isExtraCall());
    }

    @Override
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplays() {
      return call.getDestinationDisplaies();
    }

    @Override
    public ZonedDateTime getAimedArrivalTime() {
      return call.getAimedArrivalTime();
    }

    @Override
    public ZonedDateTime getExpectedArrivalTime() {
      return call.getExpectedArrivalTime();
    }

    @Override
    public ZonedDateTime getActualArrivalTime() {
      return call.getActualArrivalTime();
    }

    @Override
    public CallStatusEnumeration getArrivalStatus() {
      return null;
    }

    @Override
    public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
      return null;
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }

    @Override
    public ZonedDateTime getExpectedDepartureTime() {
      return call.getExpectedDepartureTime();
    }

    @Override
    public ZonedDateTime getActualDepartureTime() {
      return call.getActualDepartureTime();
    }

    @Override
    public CallStatusEnumeration getDepartureStatus() {
      return null;
    }

    @Override
    public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
      return null;
    }

    @Override
    public boolean isRecorded() {
      return true;
    }

    @Override
    public int hashCode() {
      return call.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RecordedCallWrapper recordedCallWrapper)) {
        return false;
      }
      return call.equals(recordedCallWrapper.call);
    }
  }
}
