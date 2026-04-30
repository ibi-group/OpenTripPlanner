package org.opentripplanner.warmup;

import graphql.ExecutionInput;
import graphql.GraphQL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.mapping.routerequest.AccessModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.EgressModeMapper;
import org.opentripplanner.apis.support.graphql.OtpDataFetcherExceptionHandler;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GtfsWarmupQueryExecutor implements WarmupQueryStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsWarmupQueryExecutor.class);

  static final String QUERY = """
    query(
      $fromLat: CoordinateValue!, $fromLon: CoordinateValue!,
      $toLat: CoordinateValue!, $toLon: CoordinateValue!,
      $dateTime: PlanDateTimeInput!,
      $accessMode: PlanAccessMode!, $egressMode: PlanEgressMode!
    ) {
      planConnection(
        origin: { location: { coordinate: { latitude: $fromLat, longitude: $fromLon } } }
        destination: { location: { coordinate: { latitude: $toLat, longitude: $toLon } } }
        dateTime: $dateTime
        modes: { transit: { access: [$accessMode], egress: [$egressMode] } }
      ) {
        edges {
          node {
            start
            end
            legs {
              mode
              duration
              from { name lat lon }
              to { name lat lon }
              route { shortName }
              legGeometry { points }
            }
          }
        }
      }
    }
    """;

  private final GraphQL graphQL;
  private final GraphQLRequestContext requestContext;
  private final ModeCombinations modeCombinations;

  GtfsWarmupQueryExecutor(
    OtpServerRequestContext context,
    List<StreetMode> accessModes,
    List<StreetMode> egressModes
  ) {
    this.requestContext = GraphQLRequestContext.ofServerContext(context);
    this.graphQL = GraphQL.newGraphQL(context.gtfsSchema())
      .defaultDataFetcherExceptionHandler(new OtpDataFetcherExceptionHandler())
      .build();
    this.modeCombinations = new ModeCombinations(accessModes, egressModes);
  }

  @Override
  public boolean execute(WgsCoordinate from, WgsCoordinate to, boolean arriveBy, int queryCount) {
    var variables = buildVariables(from, to, arriveBy, queryCount);

    var input = ExecutionInput.newExecutionInput()
      .query(QUERY)
      .context(requestContext)
      .variables(variables)
      .locale(Locale.US)
      .build();

    var result = graphQL.execute(input);
    if (!result.getErrors().isEmpty()) {
      LOG.warn("Warmup query had GraphQL errors: {}", result.getErrors());
      return false;
    }
    return true;
  }

  Map<String, Object> buildVariables(
    WgsCoordinate from,
    WgsCoordinate to,
    boolean arriveBy,
    int queryCount
  ) {
    var now = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    var dateTime = arriveBy ? Map.of("latestArrival", now) : Map.of("earliestDeparture", now);

    return Map.ofEntries(
      Map.entry("fromLat", from.latitude()),
      Map.entry("fromLon", from.longitude()),
      Map.entry("toLat", to.latitude()),
      Map.entry("toLon", to.longitude()),
      Map.entry("dateTime", dateTime),
      Map.entry("accessMode", AccessModeMapper.map(modeCombinations.access(queryCount)).name()),
      Map.entry("egressMode", EgressModeMapper.map(modeCombinations.egress(queryCount)).name())
    );
  }
}
