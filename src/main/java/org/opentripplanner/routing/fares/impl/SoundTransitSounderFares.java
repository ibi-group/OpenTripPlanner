package org.opentripplanner.routing.fares.impl;

import com.google.common.collect.ImmutableMap;
import org.opentripplanner.routing.core.Fare;

import java.util.Map;

/**
 * Class used to store all the fares for Sounder commuter rail.
 * Data comes from a Python script that parses SoundTransit's website.
 * A matrix or CSV parser would be a better approach to storing this data,
 * but a refactor is unneeded given the proximity of GTFS Fares V2 which will render this redundant.
 */
public class SoundTransitSounderFares {
    public static void populateSounderFares(Map<String, Map<Fare.FareType, Float>> linkLightRailFares) {
        linkLightRailFares.put(
            "everett-mukilteo",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "everett-edmonds",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "everett-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 5.0f, Fare.FareType.electronicRegular, 5.0f)
        );


        linkLightRailFares.put(
            "mukilteo-edmonds",
            ImmutableMap.of(Fare.FareType.regular, 3.75f, Fare.FareType.electronicRegular, 3.75f)
        );


        linkLightRailFares.put(
            "mukilteo-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.5f, Fare.FareType.electronicRegular, 4.5f)
        );


        linkLightRailFares.put(
            "edmonds-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );

        linkLightRailFares.put(
            "lakewood-southtacomadome",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "lakewood-tacomadome",
            ImmutableMap.of(Fare.FareType.regular, 3.5f, Fare.FareType.electronicRegular, 3.5f)
        );


        linkLightRailFares.put(
            "lakewood-puyallup",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "lakewood-sumner",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "lakewood-auburn",
            ImmutableMap.of(Fare.FareType.regular, 4.5f, Fare.FareType.electronicRegular, 4.5f)
        );


        linkLightRailFares.put(
            "lakewood-kent",
            ImmutableMap.of(Fare.FareType.regular, 4.75f, Fare.FareType.electronicRegular, 4.75f)
        );


        linkLightRailFares.put(
            "lakewood-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 5.0f, Fare.FareType.electronicRegular, 5.0f)
        );


        linkLightRailFares.put(
            "lakewood-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 5.75f, Fare.FareType.electronicRegular, 5.75f)
        );


        linkLightRailFares.put(
            "southtacomadome-tacomadome",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "southtacomadome-puyallup",
            ImmutableMap.of(Fare.FareType.regular, 3.75f, Fare.FareType.electronicRegular, 3.75f)
        );


        linkLightRailFares.put(
            "southtacomadome-sumner",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "southtacomadome-auburn",
            ImmutableMap.of(Fare.FareType.regular, 4.25f, Fare.FareType.electronicRegular, 4.25f)
        );


        linkLightRailFares.put(
            "southtacomadome-kent",
            ImmutableMap.of(Fare.FareType.regular, 4.5f, Fare.FareType.electronicRegular, 4.5f)
        );


        linkLightRailFares.put(
            "southtacomadome-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 5.0f, Fare.FareType.electronicRegular, 5.0f)
        );


        linkLightRailFares.put(
            "southtacomadome-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 5.5f, Fare.FareType.electronicRegular, 5.5f)
        );


        linkLightRailFares.put(
            "tacomadome-puyallup",
            ImmutableMap.of(Fare.FareType.regular, 3.5f, Fare.FareType.electronicRegular, 3.5f)
        );


        linkLightRailFares.put(
            "tacomadome-sumner",
            ImmutableMap.of(Fare.FareType.regular, 3.5f, Fare.FareType.electronicRegular, 3.5f)
        );


        linkLightRailFares.put(
            "tacomadome-auburn",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "tacomadome-kent",
            ImmutableMap.of(Fare.FareType.regular, 4.25f, Fare.FareType.electronicRegular, 4.25f)
        );


        linkLightRailFares.put(
            "tacomadome-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 4.5f, Fare.FareType.electronicRegular, 4.5f)
        );


        linkLightRailFares.put(
            "tacomadome-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 5.25f, Fare.FareType.electronicRegular, 5.25f)
        );


        linkLightRailFares.put(
            "puyallup-sumner",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "puyallup-auburn",
            ImmutableMap.of(Fare.FareType.regular, 3.5f, Fare.FareType.electronicRegular, 3.5f)
        );


        linkLightRailFares.put(
            "puyallup-kent",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "puyallup-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 4.25f, Fare.FareType.electronicRegular, 4.25f)
        );


        linkLightRailFares.put(
            "puyallup-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.75f, Fare.FareType.electronicRegular, 4.75f)
        );


        linkLightRailFares.put(
            "sumner-auburn",
            ImmutableMap.of(Fare.FareType.regular, 3.5f, Fare.FareType.electronicRegular, 3.5f)
        );


        linkLightRailFares.put(
            "sumner-kent",
            ImmutableMap.of(Fare.FareType.regular, 3.75f, Fare.FareType.electronicRegular, 3.75f)
        );


        linkLightRailFares.put(
            "sumner-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "sumner-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.75f, Fare.FareType.electronicRegular, 4.75f)
        );


        linkLightRailFares.put(
            "auburn-kent",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "auburn-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 3.75f, Fare.FareType.electronicRegular, 3.75f)
        );


        linkLightRailFares.put(
            "auburn-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.25f, Fare.FareType.electronicRegular, 4.25f)
        );


        linkLightRailFares.put(
            "kent-tukwila",
            ImmutableMap.of(Fare.FareType.regular, 3.25f, Fare.FareType.electronicRegular, 3.25f)
        );


        linkLightRailFares.put(
            "kent-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 4.0f, Fare.FareType.electronicRegular, 4.0f)
        );


        linkLightRailFares.put(
            "tukwila-kingstreet",
            ImmutableMap.of(Fare.FareType.regular, 3.75f, Fare.FareType.electronicRegular, 3.75f)
        );

    }
}
