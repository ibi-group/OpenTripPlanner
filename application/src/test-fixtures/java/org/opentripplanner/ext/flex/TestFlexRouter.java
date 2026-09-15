package org.opentripplanner.ext.flex;

import java.util.List;
import org.opentripplanner.model.plan.ItinerarySummarizer;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.service.streetdetails.NoopStreetDetailsService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.site.StopLocation;

public class TestFlexRouter {

  private final TransitTestEnvironment env;
  private NearbyStop start;
  private NearbyStop end;

  private TestFlexRouter(TransitTestEnvironment env) {
    this.env = env;
  }

  public static TestFlexRouter of(TransitTestEnvironment env) {
    return new TestFlexRouter(env);
  }

  public TestFlexRouter withStart(StopLocation s) {
    start = NearbyStop.ofZeroDistance(s.getId(), TestStateBuilder.ofWalking().build());
    return this;
  }

  public TestFlexRouter withEnd(StopLocation s) {
    end = NearbyStop.ofZeroDistance(s.getId(), TestStateBuilder.ofWalking().build());
    return this;
  }

  public ItinerarySummarizer routeDirect(String time) {
    var router = new FlexRouter(
      new Graph(),
      env.transitService(),
      TransferServiceTestFactory.defaultTransferService(),
      new NoopStreetDetailsService(),
      FlexParameters.defaultValues(),
      TripRequest.of().build(),
      env.localTimeParser().instant(time),
      null,
      1,
      1,
      List.of(start),
      List.of(end)
    );

    var results = router.createFlexOnlyItineraries(false, RouteRequest.defaultValue());

    return new ItinerarySummarizer(results);
  }
}
