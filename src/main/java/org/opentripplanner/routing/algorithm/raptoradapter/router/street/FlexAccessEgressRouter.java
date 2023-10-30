package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
    RouteRequest request,
    TemporaryVerticesContainer verticesContainer,
    OtpServerRequestContext serverContext,
    AdditionalSearchDays searchDays,
    AccessEgressType type
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    var overlay = serverContext.dataOverlayContext(request);
    TransitService transitService = serverContext.transitService();

    Collection<NearbyStop> accessStops = type.isAccess()
      ? AccessEgressRouter.streetSearch(
        request,
        verticesContainer,
        transitService,
        new StreetRequest(StreetMode.WALK),
        overlay,
        false,
        serverContext.flexConfig().maxAccessWalkDuration(),
        request.preferences().street().accessEgress().maxStopCount()
      )
      : List.of();

    Collection<NearbyStop> egressStops = type.isEgress()
      ? AccessEgressRouter.streetSearch(
        request,
        verticesContainer,
        transitService,
        new StreetRequest(StreetMode.WALK),
        overlay,
        true,
        serverContext.flexConfig().maxEgressWalkDuration(),
        request.preferences().street().accessEgress().maxStopCount()
      )
      : List.of();

    FlexRouter flexRouter = new FlexRouter(
      serverContext.graph(),
      transitService,
      serverContext.flexConfig(),
      request.dateTime(),
      request.arriveBy(),
      searchDays.additionalSearchDaysInPast(),
      searchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return switch (type) {
      case ACCESS -> flexRouter.createFlexAccesses();
      case EGRESS -> flexRouter.createFlexEgresses();
    };
  }
}
