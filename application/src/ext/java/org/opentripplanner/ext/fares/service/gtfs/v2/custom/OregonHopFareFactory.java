package org.opentripplanner.ext.fares.service.gtfs.v2.custom;


import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.ext.fares.service.gtfs.GtfsFaresService;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.ext.fares.service.gtfs.v2.GtfsFaresV2Service;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.Money;

public class OregonHopFareFactory extends DefaultFareServiceFactory {
  /**
   * Generates fare products based on C-TRAN/TriMet data. Relies on 2 fares per rider category.
   * To calculate the effective fare, the second fare is subtracted from the first.
   */
  private Collection<FareProduct> generateHopFareProducts(Money AdultFirst, Money AdultSecond, Money SeniorFirst, Money SeniorSecond, Money YouthFirst, Money YouthSecond) {
    final Collection<FareProduct> hopFareProducts = new HashSet<>();

    // Adult
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
          AdultFirst.minus(AdultSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
          AdultFirst.minus(AdultSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
          AdultFirst.minus(AdultSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );

    // Senior
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HC_TRANSFER"),
          "TriMet to C-TRAN",
          SeniorFirst.minus(SeniorSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HONORED_CITIZEN_TRANSFER"),
          "TriMet to C-TRAN",
          SeniorFirst.minus(SeniorSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HONORED_CITIZEN_TRANSFER"),
          "TriMet to C-TRAN",
          SeniorFirst.minus(SeniorSecond))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );


    // Youth
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthFirst.minus(YouthSecond)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthFirst.minus(YouthSecond)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthFirst.minus(YouthSecond)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );

    return hopFareProducts;
  }

  public FareService makeFareService() {
    DefaultFareService fareService = new DefaultFareService();
    fareService.addFareRules(FareType.regular, regularFareRules.values());

    // TODO: It might be nice to extract these prices out dynamically from the data
    final Money ADULT_TRIMET = Money.usDollars(2.8f);
    final Money REDUCED_TRIMET = Money.usDollars(1.4f);
    final Money ADULT_STREETCAR = Money.usDollars(2f);
    final Money REDUCED_STREETCAR = Money.usDollars(1f);

    final Money CTRAN_EXP = Money.usDollars(3.25f);

    final Money ADULT_CTRAN_REGIONAL = Money.usDollars(2.8f);
    final Money REDUCED_CTRAN_REGIONAL = Money.usDollars(1.4f);

    final Money ADULT_CTRAN_LOCAL = Money.usDollars(1.5f);
    final Money REDUCED_CTRAN_LOCAL = Money.usDollars(0.75f);


    // TODO: c-tran to flex
    // TODO: trimet to flex

    // TriMet to C-TRAN
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "trimet-to-ctran-regional"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_CTRAN_REGIONAL, ADULT_TRIMET, REDUCED_CTRAN_REGIONAL, REDUCED_TRIMET, REDUCED_CTRAN_REGIONAL, REDUCED_TRIMET))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "trimet-to-ctran-local"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_CTRAN_LOCAL, ADULT_TRIMET, REDUCED_CTRAN_LOCAL, REDUCED_TRIMET, Money.ZERO_USD, REDUCED_TRIMET))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "trimet-to-ctran-express"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(CTRAN_EXP, ADULT_TRIMET, CTRAN_EXP, REDUCED_TRIMET, CTRAN_EXP, REDUCED_TRIMET))
      .build());

    // PSC to C-TRAN
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "psc-to-ctran-regional"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_CTRAN_REGIONAL, ADULT_STREETCAR, REDUCED_CTRAN_REGIONAL, REDUCED_STREETCAR, REDUCED_CTRAN_REGIONAL, REDUCED_STREETCAR))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "psc-to-ctran-local"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_CTRAN_LOCAL, ADULT_STREETCAR, REDUCED_CTRAN_LOCAL, REDUCED_STREETCAR, Money.ZERO_USD, REDUCED_STREETCAR))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "psc-to-ctran-express"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(CTRAN_EXP, ADULT_STREETCAR, CTRAN_EXP, REDUCED_STREETCAR, CTRAN_EXP, REDUCED_STREETCAR))
      .build());

    // C-TRAN to TriMet
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-regional-to-trimet"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_TRIMET, ADULT_CTRAN_REGIONAL, REDUCED_TRIMET, REDUCED_CTRAN_REGIONAL, REDUCED_TRIMET, REDUCED_CTRAN_REGIONAL))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-local-to-trimet"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_TRIMET, ADULT_CTRAN_LOCAL, REDUCED_TRIMET, REDUCED_CTRAN_LOCAL, REDUCED_TRIMET, Money.ZERO_USD))
      .build());

    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-express-to-trimet"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());

    // C-TRAN to PSC
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-regional-to-psc"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_STREETCAR, ADULT_CTRAN_REGIONAL, REDUCED_STREETCAR, REDUCED_CTRAN_REGIONAL, REDUCED_STREETCAR, REDUCED_CTRAN_REGIONAL))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-local-to-psc"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_STREETCAR, ADULT_CTRAN_LOCAL, REDUCED_STREETCAR, REDUCED_CTRAN_LOCAL, REDUCED_STREETCAR, Money.ZERO_USD))
      .build());

    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("TRIMET", "ctran-express-to-psc"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());

    // The C-TRAN Data fails to include unlimited free transfers. This corrects the data. TODO: is this correct?
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "EXP_TO_LOCAL_UNLIMITED"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "EXP_TO_REGIONAL_UNLIMITED"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "REGIONAL_TO_LOCAL_UNLIMITED"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());


    var faresV2Service = GtfsFaresV2Service.of()
      .withLegRules(this.fareLegRules)
      .withTransferRules(this.fareTransferRules)
      .withStopAreas(this.stopAreas)
      .withServiceIds(this.serviceDates)
      .build();

    return new GtfsFaresService(fareService, faresV2Service);
  }
}
