package org.opentripplanner.updater.impedance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.mobilityprofile.MobilityProfile;
import org.opentripplanner.ext.mobilityprofile.MobilityProfileData;

class OsmImpedanceUpdaterTest {
  private MobilityProfileData profileData(float impNone, float impWChairE, float impBlind) {
    return new MobilityProfileData(10, 123456, 654321, Map.of(
      MobilityProfile.NONE, impNone,
      MobilityProfile.WCHAIRE, impWChairE,
      MobilityProfile.BLIND, impBlind
    ));
  }

  @Test
  void areSameImpedances() {
    MobilityProfileData profileData1 = profileData(1.2F, 5.0F, 3.4F);
    MobilityProfileData profileData2 = profileData(1.6F, 5.0F, 3.4F);
    MobilityProfileData profileData3 = profileData(1.2F, 5.0F, 3.4F);

    assertTrue(OsmImpedanceUpdater.areSameImpedances(profileData1, profileData1));
    assertFalse(OsmImpedanceUpdater.areSameImpedances(profileData1, profileData2));
    assertTrue(OsmImpedanceUpdater.areSameImpedances(profileData1, profileData3));
  }

  /**
   * Contains a simple case for filtering impedances.
   * Impedance data are simplified to three profiles: "None", "WChairE", and "Blind".
   * OSM attributes such as way ids and corresponding geometry are assumed identical.
   */
  @Test
  void getChangedImpedances() {
    Map<String, MobilityProfileData> newImpedances = Map.of(
      "Street1", profileData(1.2F, 5.0F, 3.4F),
      "Street2", profileData(2.1F, 7.0F, 4.4F),
      "Street3", profileData(0.6F, 1.1F, 1.5F),
      "Street4", profileData(1.4F, 1.6F, 1.8F),
      "Street6", profileData(3.1F, 2.6F, 4.0F)
    );
    Map<String, MobilityProfileData> oldImpedances = Map.of(
      "Street1", profileData(1.2F, 5.0F, 3.4F),
      "Street2", profileData(2.1F, 3.0F, 4.4F),
      "Street3", profileData(0.6F, 1.1F, 1.5F),
      "Street4", profileData(1.4F, 1.6F, 1.8F),
      "Street5", profileData(2.3F, 2.5F, 3.0F)
    );

    Map<String, MobilityProfileData> changedImpedances = Map.of(
      "Street6", profileData(3.1F, 2.6F, 4.0F),
      "Street5", new MobilityProfileData(10, 123456, 654321, Map.of()),
      "Street2", profileData(2.1F, 7.0F, 4.4F)
    );

    assertEquals(changedImpedances, OsmImpedanceUpdater.getChangedImpedances(newImpedances, oldImpedances));
  }
}
