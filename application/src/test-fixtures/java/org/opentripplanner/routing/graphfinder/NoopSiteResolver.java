package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;

public class NoopSiteResolver implements SiteResolver {

  @Override
  public RegularStop getStop(FeedScopedId id) {
    return null;
  }

  @Override
  public Entrance getEntrance(FeedScopedId id) {
    return null;
  }
}
