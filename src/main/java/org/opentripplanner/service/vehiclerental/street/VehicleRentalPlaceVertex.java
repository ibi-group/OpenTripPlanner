package org.opentripplanner.service.vehiclerental.street;

import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A vertex for a rental vehicle or station. It is connected to the streets by a
 * {@link StreetVehicleRentalLink}. To allow transitions on and
 * off a vehicle, it has {@link VehicleRentalEdge} loop edges.
 */
public class VehicleRentalPlaceVertex extends Vertex {

  private VehicleRentalPlace station;

  public VehicleRentalPlaceVertex(VehicleRentalPlace station) {
    super(
      "vehicle rental station " + station.getId(),
      station.getLongitude(),
      station.getLatitude(),
      station.getName()
    );
    this.station = station;
  }

  public VehicleRentalPlace getStation() {
    return station;
  }

  public void setStation(VehicleRentalPlace station) {
    this.station = station;
  }
}
