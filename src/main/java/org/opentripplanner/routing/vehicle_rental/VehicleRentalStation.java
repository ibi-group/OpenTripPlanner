package org.opentripplanner.routing.vehicle_rental;

import static java.util.Locale.ROOT;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.I18NString;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class VehicleRentalStation implements VehicleRentalPlace {

    // GBFS  Static information
    public FeedScopedId id;
    public I18NString name;
    public String shortName;
    public double longitude;
    public double latitude;
    public String address;
    public String crossStreet;
    public String regionId;
    public String postCode;
    public Set<String> rentalMethods;
    public boolean isVirtualStation = false;
    public Geometry stationArea;
    public Integer capacity;
    public Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity;
    public Map<RentalVehicleType, Integer> vehicleTypeDockCapacity;
    public boolean isValetStation = false;
    public VehicleRentalSystem system;
    public VehicleRentalStationUris rentalUris;

    // GBFS Dynamic information
    public int vehiclesAvailable = 0;
    public int vehiclesDisabled = 0;
    public Map<RentalVehicleType, Integer> vehicleTypesAvailable = Map.of();
    public int spacesAvailable = 0;
    public int spacesDisabled = 0;
    public Map<RentalVehicleType, Integer> vehicleSpacesAvailable = Map.of();

    public boolean isInstalled = true;
    public boolean isRenting = true;
    public boolean isReturning = true;
    public ZonedDateTime lastReported;

    // OTP internal data
    public boolean isKeepingVehicleRentalAtDestinationAllowed = false;
    public boolean realTimeData = true;


    @Override
    public FeedScopedId getId() {
        return id;
    }

    @Override
    public String getStationId() {
        return getId().getId();
    }

    @Override
    public String getNetwork() {
        return getId().getFeedId();
    }


    @Override
    public I18NString getName() {
        return name;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public int getVehiclesAvailable() {
        return vehiclesAvailable;
    }

    @Override
    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    @Override
    public boolean isAllowDropoff() {
        return isReturning;
    }

    public boolean allowPickupNow() {
        return isRenting && vehiclesAvailable > 0;
    }

    public boolean allowDropoffNow() {
        return isReturning && spacesAvailable > 0;
    }

    @Override
    public boolean isFloatingBike() {
        return false;
    }

    @Override
    public boolean isCarStation() {
        return Stream.concat(
                vehicleTypesAvailable.keySet().stream(),
                vehicleSpacesAvailable.keySet().stream()
            )
            .anyMatch(rentalVehicleType -> rentalVehicleType.formFactor.equals(RentalVehicleType.FormFactor.CAR));
    }

    @Override
    public boolean isKeepingVehicleRentalAtDestinationAllowed() {
        return isKeepingVehicleRentalAtDestinationAllowed;
    }

    @Override
    public boolean isRealTimeData() {
        return realTimeData;
    }

    @Override
    public VehicleRentalStationUris getRentalUris() {
        return rentalUris;
    }

    @Override
    public String toString () {
        return String.format(ROOT, "Vehicle rental station %s at %.6f, %.6f", name, latitude, longitude);
    }
}
