package org.opentripplanner.transit.model.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.CompactLineStringUtils;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Encapsulates the per-hop geometry of a {@link TripPattern} together with a precomputed
 * cumulative distance table. Holding both fields together lets callers look up the distance
 * between any two stop positions in O(1) instead of re-running the haversine sum over the
 * pattern geometry on every leg construction.
 * <p>
 * When no shape data is available at construction, the per-hop geometry is materialized as a
 * straight line between consecutive stops and compressed through the same pipeline as real shape
 * data. This matches the behaviour the importers already expose ({@link
 * org.opentripplanner.netex.mapping.ServiceLinkMapper} for NeTEx, the GTFS {@link
 * org.opentripplanner.graph_builder.module.geometry.GeometryProcessor#createHopGeometries} for
 * GTFS), so {@link #concatenatedGeometry()} returns a non-null geometry for any pattern with at
 * least one hop, regardless of whether the source data carried a shape.
 */
final class TripPatternGeometry implements Serializable {

  /**
   * Compressed per-hop geometry, one entry per hop (length = numberOfStops - 1). Always
   * non-null. For shapeless patterns the entries are the compressed form of synthetic
   * 2-point straight lines between consecutive stops.
   */
  private final byte[][] hopGeometries;

  /**
   * Cumulative distance in meters along the pattern from stop 0 up to stop i.
   * Length = numberOfStops; entry 0 is 0.
   */
  private final int[] cumulativeDistanceMeters;

  private TripPatternGeometry(byte[][] hopGeometries, int[] cumulativeDistanceMeters) {
    this.hopGeometries = hopGeometries;
    this.cumulativeDistanceMeters = cumulativeDistanceMeters;
  }

  /**
   * Build a {@link TripPatternGeometry} for the given stop pattern. When {@code hopGeometries}
   * is non-null the per-hop distances are summed over each line string; otherwise the straight
   * line between consecutive stops is materialized from the stop coordinates. Distances are
   * summed in double and rounded to the nearest meter only when writing each entry of the
   * cumulative table, which bounds the rounding error of any
   * {@code distanceBetween(board, alight)} query to at most 1 meter independent of leg length.
   */
  static TripPatternGeometry of(StopPattern stopPattern, @Nullable List<LineString> hopGeometries) {
    int numberOfStops = stopPattern.getSize();
    int expectedHops = Math.max(numberOfStops - 1, 0);
    if (hopGeometries != null && hopGeometries.size() != expectedHops) {
      throw new IllegalArgumentException(
        "hopGeometries size (%d) does not match the number of hops in the stop pattern (%d)".formatted(
          hopGeometries.size(),
          expectedHops
        )
      );
    }
    double[] cumulativeDouble = new double[numberOfStops];
    byte[][] compressed = new byte[expectedHops][];

    if (hopGeometries != null) {
      for (int i = 0; i < hopGeometries.size(); i++) {
        LineString hop = hopGeometries.get(i);
        cumulativeDouble[i + 1] =
          cumulativeDouble[i] + GeometryUtils.sumDistances(hop.getCoordinateSequence());
        compressed[i] = CompactLineStringUtils.compactLineString(hop, false);
      }
    } else {
      for (int i = 0; i < numberOfStops - 1; i++) {
        StopLocation from = stopPattern.getStop(i);
        StopLocation to = stopPattern.getStop(i + 1);
        LineString hop = GeometryUtils.makeLineString(from.getCoordinate(), to.getCoordinate());
        cumulativeDouble[i + 1] =
          cumulativeDouble[i] +
          SphericalDistanceLibrary.distance(from.getLat(), from.getLon(), to.getLat(), to.getLon());
        compressed[i] = CompactLineStringUtils.compactLineString(hop, false);
      }
    }

    int[] cumulativeMeters = new int[numberOfStops];
    for (int i = 0; i < numberOfStops; i++) {
      cumulativeMeters[i] = (int) Math.round(cumulativeDouble[i]);
    }
    return new TripPatternGeometry(compressed, cumulativeMeters);
  }

  /**
   * Distance in meters along the pattern between the two stop positions. Constant time.
   */
  int distanceBetween(int boardingStopPosition, int alightingStopPosition) {
    return (
      cumulativeDistanceMeters[alightingStopPosition] -
      cumulativeDistanceMeters[boardingStopPosition]
    );
  }

  /**
   * Return the geometry of the hop at {@code hopIndex}. For shapeless patterns the returned
   * geometry is the synthetic straight line between the consecutive stop coordinates, which
   * matches the previous on-the-fly synthesis behaviour.
   */
  LineString hopGeometry(int hopIndex) {
    return CompactLineStringUtils.uncompactLineString(hopGeometries[hopIndex], false);
  }

  /**
   * Return the concatenated hop geometry between the boarding and alighting stop positions.
   * Symmetric with {@link #distanceBetween(int, int)}.
   */
  LineString geometryBetween(int boardingStopPosition, int alightingStopPosition) {
    List<LineString> hops = new ArrayList<>(alightingStopPosition - boardingStopPosition);
    for (int i = boardingStopPosition; i < alightingStopPosition; i++) {
      hops.add(hopGeometry(i));
    }
    return GeometryUtils.concatenateLineStrings(hops);
  }

  /**
   * Return the concatenated geometry of all hops. For shapeless patterns the returned geometry is
   * composed of the synthetic straight lines between consecutive stops; for degenerate patterns
   * with no hops (one stop or fewer) the returned geometry is empty but never null.
   */
  LineString concatenatedGeometry() {
    return geometryBetween(0, hopGeometries.length);
  }
}
