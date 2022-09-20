package org.opentripplanner.routing.core.intersection_model;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * An interface to a model that computes the duration of turns.
 * <p>
 * Turn durations are given in seconds - they represent the expected amount of time it would take to
 * make a turn.
 *
 * @author avi
 */
public interface IntersectionTraversalCalculator {
  /**
   * Compute the duration of turning onto "to" from "from".
   *
   * @return expected number of seconds the traversal is expected to take.
   */
  double computeTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to,
    TraverseMode mode,
    float fromSpeed,
    float toSpeed
  );

  static IntersectionTraversalCalculator create(
    IntersectionTraversalModel intersectionTraversalModel,
    DrivingDirection drivingDirection
  ) {
    return switch (intersectionTraversalModel) {
      case NORWAY -> new NorwayIntersectionTraversalCalculator(drivingDirection);
      case SIMPLE -> new SimpleIntersectionTraversalCalculator(drivingDirection);
    };
  }
}
