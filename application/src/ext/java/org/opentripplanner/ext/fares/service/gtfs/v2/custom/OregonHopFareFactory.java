package org.opentripplanner.ext.fares.service.gtfs.v2.custom;


import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
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

  private FareProduct findFareProduct(FeedScopedId fareProductId) {
    Optional<FareLegRule> potentialRuleMatch = this.fareLegRules.stream()
      .filter(f ->  f.fareProducts().stream().anyMatch(fp -> fp.id().equals(fareProductId)))
      .findFirst();

    return potentialRuleMatch
      .flatMap(flr -> flr.fareProducts().stream().filter(fp -> fp.id().equals(fareProductId)).findFirst())
      .orElse(FareProduct.of(fareProductId, "Could not find fare in data", Money.ZERO_USD).build());
  }

  /**
   * Generates fare products based on C-TRAN/TriMet data. Relies on 2 fares per rider category.
   * To calculate the effective fare, the second fare is subtracted from the first.
   */
  private Collection<FareProduct> generateHopFareProducts(Money AdultLarger, Money AdultSmaller, Money SeniorLarger, Money SeniorSmaller, Money YouthLarger, Money YouthSmaller) {
    final Collection<FareProduct> hopFareProducts = new HashSet<>();


    // Adult
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, AdultLarger.minus(AdultSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, AdultLarger.minus(AdultSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_ADULT_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, AdultLarger.minus(AdultSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:ADULT")).withName("Adult").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );

    // Senior
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HC_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, (SeniorLarger.minus(SeniorSmaller))))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HONORED_CITIZEN_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, SeniorLarger.minus(SeniorSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_HONORED_CITIZEN_TRANSFER"),
          "TriMet to C-TRAN",
        Money.max(Money.ZERO_USD, SeniorLarger.minus(SeniorSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:HONORED_CITIZEN")).withName("Honored Citizen").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );


    // Youth
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthLarger.minus(YouthSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:2"), "HOP Fastpass"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthLarger.minus(YouthSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:3"), "Open Payment"))
        .build()
    );
    hopFareProducts.add(
      FareProduct.of(
          new FeedScopedId("CTRAN", "TRIMET_CTRAN_YOUTH_TRANSFER"),
          "TriMet to C-TRAN",
          Money.max(Money.ZERO_USD, YouthLarger.minus(YouthSmaller)))
        .withCategory(RiderCategory.of(FeedScopedId.parse("CTRAN:YOUTH")).withName("Youth").build())
        .withMedium(new FareMedium(FeedScopedId.parse("CTRAN:4"), "Virtual HOP Fastpass"))
        .build()
    );

    return hopFareProducts;
  }

  public FareService makeFareService() {
    DefaultFareService fareService = new DefaultFareService();
    fareService.addFareRules(FareType.regular, regularFareRules.values());

    final Money ADULT_TRIMET = findFareProduct(new FeedScopedId("TRIMET","TRIMET_ADULT_SINGLE_RIDE")).price();
    final Money REDUCED_TRIMET = findFareProduct(new FeedScopedId("TRIMET","TRIMET_HC_SINGLE_RIDE")).price();
    final Money ADULT_STREETCAR = findFareProduct(new FeedScopedId("TRIMET","STREETCAR_ADULT_SINGLE_RIDE")).price();
    final Money REDUCED_STREETCAR = findFareProduct(new FeedScopedId("TRIMET","STREETCAR_HC_SINGLE_RIDE")).price();

    final Money CTRAN_EXP = findFareProduct(new FeedScopedId("CTRAN", "ADULT_EXPRESS_SINGLE_RIDE")).price();

    final Money ADULT_CTRAN_REGIONAL = findFareProduct(new FeedScopedId("CTRAN", "ADULT_REGIONAL_SINGLE_RIDE")).price();
    final Money REDUCED_CTRAN_REGIONAL = findFareProduct(new FeedScopedId("CTRAN", "HC_REGIONAL_SINGLE_RIDE")).price();

    final Money ADULT_CTRAN_LOCAL = findFareProduct(new FeedScopedId("CTRAN", "ADULT_LOCAL_SINGLE_RIDE")).price();
    final Money REDUCED_CTRAN_LOCAL = findFareProduct(new FeedScopedId("CTRAN", "HC_LOCAL_SINGLE_RIDE")).price();


    // TODO: Senior discounted express upcharge during the mornings
    // Handle senior off-peak fares
    // FareProduct seniorOffPeakExpress = findFareProduct(new FeedScopedId("CTRAN", "HC_EXPRESS_SINGLE_RIDE_MIDDAY"));
    // Money CTRAN_EXP_SENIOR_OFFPEAK = seniorOffPeakExpress.price();



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
      .withId(new FeedScopedId("CTRAN", "trimet-to-ctran-flex"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withToLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
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
      .withId(new FeedScopedId("CTRAN", "psc-to-ctran-flex"))
      .withFromLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withToLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
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
      .withId(new FeedScopedId("TRIMET", "ctran-flex-to-trimet"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:TRIMET"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
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
      .withId(new FeedScopedId("TRIMET", "ctran-flex-to-psc"))
      .withToLegGroup(FeedScopedId.parse("TRIMET:PSC"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
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



    // CTRAN to CTRAN Flex transfers
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "LOCAL_TO_FLEX"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "REGIONAL_TO_FLEX"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "EXPRESS_TO_FLEX"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withToLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());

    // CTRAN Flex to CTRAN transfers
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "FLEX_TO_LOCAL"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:LOCAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "FLEX_TO_REGIONAL"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:REGIONAL"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(ADULT_CTRAN_REGIONAL, ADULT_CTRAN_LOCAL, REDUCED_CTRAN_REGIONAL, REDUCED_CTRAN_LOCAL, REDUCED_CTRAN_REGIONAL, Money.ZERO_USD))
      .build());
    this.fareTransferRules.add(FareTransferRule.of()
      .withId(new FeedScopedId("CTRAN", "FLEX_TO_EXPRESS"))
      .withFromLegGroup(FeedScopedId.parse("CTRAN_FLEX:LOCAL"))
      .withToLegGroup(FeedScopedId.parse("CTRAN:EXPRESS"))
      .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
      .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(180))
      .withFareProducts(generateHopFareProducts(CTRAN_EXP, ADULT_CTRAN_LOCAL, CTRAN_EXP, REDUCED_CTRAN_LOCAL, CTRAN_EXP, Money.ZERO_USD))
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
