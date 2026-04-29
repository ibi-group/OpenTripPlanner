package org.opentripplanner.routing.algorithm;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Pre-processes a {@link RouteRequest} into a {@link RoutingWorkerRequest} for usage in
 * {@link RoutingWorker}. This includes extending the request with a page cursor and amending
 * its data with additional resolved data, such as additional search days.
 */
public final class RequestPreProcessor {

  private final RaptorTuningParameters tuningParameters;
  private final ZoneId zoneId;

  public RequestPreProcessor(RaptorTuningParameters tuningParameters, ZoneId zoneId) {
    this.tuningParameters = tuningParameters;
    this.zoneId = zoneId;
  }

  /**
   * Pre-process {@code originalRequest} and return a {@link RoutingWorkerRequest} with all values
   * needed to initialize a {@link RoutingWorker}.
   */
  public RoutingWorkerRequest computeRequest(RouteRequest originalRequest) {
    var request = originalRequest.withPageCursor();
    var transitSearchTimeZero = ServiceDateUtils.asStartOfService(request.dateTime(), zoneId);
    var additionalSearchDays = createAdditionalSearchDays(tuningParameters, zoneId, request);
    return new RoutingWorkerRequest(request, transitSearchTimeZero, additionalSearchDays);
  }

  private static AdditionalSearchDays createAdditionalSearchDays(
    RaptorTuningParameters raptorTuningParameters,
    ZoneId zoneId,
    RouteRequest request
  ) {
    var dateTime = Objects.requireNonNull(request.dateTime());
    var searchDateTime = ZonedDateTime.ofInstant(dateTime, zoneId);
    var maxWindow = raptorTuningParameters.dynamicSearchWindowCoefficients().maxWindow();

    return new AdditionalSearchDays(
      request.arriveBy(),
      searchDateTime,
      request.searchWindow(),
      maxWindow,
      request.preferences().system().maxJourneyDuration()
    );
  }
}
