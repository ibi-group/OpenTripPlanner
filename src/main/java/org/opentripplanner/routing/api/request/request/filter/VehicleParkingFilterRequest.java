package org.opentripplanner.routing.api.request.request.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

/**
 * A request object that checks if parking faclities match certain conditions for
 * inclusion/exclusion or preference/unpreference.
 */
public class VehicleParkingFilterRequest {

  private final VehicleParkingFilter[] not;
  private final VehicleParkingFilter[] select;

  public VehicleParkingFilterRequest(
    Collection<VehicleParkingFilter> not,
    Collection<VehicleParkingFilter> select
  ) {
    this.not = makeFilter(not);
    this.select = makeFilter(select);
  }

  public VehicleParkingFilterRequest(VehicleParkingFilter not, VehicleParkingFilter select) {
    this(List.of(not), List.of(select));
  }

  /**
   * Create a request with no conditions.
   */
  public static VehicleParkingFilterRequest empty() {
    return new VehicleParkingFilterRequest(List.of(), List.of());
  }

  /**
   * Checks if a parking facility matches the conditions defined in this filter.
   */
  public boolean matches(VehicleParking p) {
    for (var n : not) {
      if (n.matches(p)) {
        return false;
      }
    }
    // not doesn't match and no selects means it matches
    if (select.length == 0) {
      return true;
    }
    for (var s : select) {
      if (s.matches(p)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addCol("not", Arrays.asList(not))
      .addCol("select", Arrays.asList(select))
      .toString();
  }

  @Nonnull
  private static VehicleParkingFilter[] makeFilter(Collection<VehicleParkingFilter> select) {
    return select.stream().filter(f -> !f.isEmpty()).toArray(VehicleParkingFilter[]::new);
  }
}
