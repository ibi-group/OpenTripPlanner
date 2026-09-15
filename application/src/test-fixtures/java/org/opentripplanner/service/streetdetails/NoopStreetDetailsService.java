package org.opentripplanner.service.streetdetails;

import java.util.Optional;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.edge.Edge;

public class NoopStreetDetailsService implements StreetDetailsService {

  @Override
  public Optional<InclinedEdgeLevelInfo> findInclinedEdgeLevelInfo(Edge edge) {
    return Optional.empty();
  }

  @Override
  public Optional<Level> findHorizontalEdgeLevelInfo(Edge edge) {
    return Optional.empty();
  }
}
