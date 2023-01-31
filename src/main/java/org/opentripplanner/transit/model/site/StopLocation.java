package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred to in stop times.
 */
public interface StopLocation extends LogInfo {
  AtomicInteger INDEX_COUNTER = new AtomicInteger(0);

  /** The ID for the StopLocation */
  FeedScopedId getId();

  /**
   * This is the OTP internal <em>synthetic key</em>, used to reference a StopLocation inside OTP.  This is used
   * to optimize routing, we do not access the stop instance only keep the {code index}. The index will not change.
   * <p>
   * Do NOT expose this index in the APIs, it is not guaranteed to be the same across different OTP instances,
   * use the {code id} for external references.
   */
  int getIndex();

  /** Name of the StopLocation, if provided */
  @Nullable
  I18NString getName();

  @Nullable
  I18NString getDescription();

  @Nullable
  I18NString getUrl();

  /**
   * Short text or a number that identifies the location for riders. These codes are often used in
   * phone-based reservation systems to make it easier for riders to specify a particular location.
   * The stop_code can be the same as id if it is public facing. This field should be left empty for
   * locations without a code presented to riders.
   */
  @Nullable
  default String getCode() {
    return null;
  }

  @Nullable
  default String getPlatformCode() {
    return null;
  }

  @Nullable
  default TransitMode getGtfsVehicleType() {
    return null;
  }

  @Nonnull
  default SubMode getNetexVehicleSubmode() {
    return SubMode.UNKNOWN;
  }

  default double getLat() {
    return getCoordinate().latitude();
  }

  default double getLon() {
    return getCoordinate().longitude();
  }

  @Nullable
  default Station getParentStation() {
    return null;
  }

  @Nonnull
  default Collection<FareZone> getFareZones() {
    return List.of();
  }

  @Nonnull
  default Accessibility getWheelchairAccessibility() {
    return Accessibility.NO_INFORMATION;
  }

  /**
   * This is to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop.
   */
  @Nullable
  default String getFirstZoneAsString() {
    for (FareZone t : getFareZones()) {
      return t.getId().getId();
    }
    return null;
  }

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  @Nonnull
  WgsCoordinate getCoordinate();

  /**
   * Returns coordinate rounded to 4 decimal places (which is an accuracy of ~10m)
   */
  public default WgsCoordinate getRoundedCoordinate() {
    double scale = Math.pow(10, 3);
    WgsCoordinate newCoord = new WgsCoordinate(Math.round(this.getCoordinate().latitude() * scale) / scale, Math.round(this.getCoordinate().longitude() * scale) / scale);
    return newCoord;
  }

  /**
   * The geometry of the stop.
   * <p>
   * For fixed-schedule stops this will return the same data as getCoordinate().
   * <p>
   * For flex stops this will return the geometries of the stop or group of stops.
   */
  @Nullable
  Geometry getGeometry();

  @Nullable
  default ZoneId getTimeZone() {
    return null;
  }

  boolean isPartOfStation();

  @Nonnull
  default StopTransferPriority getPriority() {
    return StopTransferPriority.ALLOWED;
  }

  boolean isPartOfSameStationAs(StopLocation alternativeStop);

  @Override
  default String logName() {
    return ObjectUtils.ifNotNull(getName(), Object::toString, null);
  }

  /**
   * Get the parent station id if such exists. Otherwise, return the stop id.
   */
  default FeedScopedId getStationOrStopId() {
    if (this instanceof StationElement<?, ?> stationElement && stationElement.isPartOfStation()) {
      return stationElement.getParentStation().getId();
    }
    return getId();
  }

  /**
   * Whether we should allow transfers to and from stop location (other than transit)
   */
  default boolean transfersNotAllowed() {
    return false;
  }

  static int indexCounter() {
    return INDEX_COUNTER.get();
  }

  /**
   * Use this ONLY when deserializing the graph. Sets the counter value to the highest recorded value
   */
  static void initIndexCounter(int indexCounter) {
    INDEX_COUNTER.set(indexCounter);
  }
}
