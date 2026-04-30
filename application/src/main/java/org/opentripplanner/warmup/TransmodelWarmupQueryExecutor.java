package org.opentripplanner.warmup;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.support.AbortOnUnprocessableRequestExecutionStrategy;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransmodelWarmupQueryExecutor implements WarmupQueryStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelWarmupQueryExecutor.class);

  static final String QUERY = """
    query(
      $fromLat: Float!, $fromLon: Float!,
      $toLat: Float!, $toLon: Float!,
      $arriveBy: Boolean!,
      $accessMode: StreetMode!, $egressMode: StreetMode!
    ) {
      trip(
        from: { coordinates: { latitude: $fromLat, longitude: $fromLon } }
        to: { coordinates: { latitude: $toLat, longitude: $toLon } }
        arriveBy: $arriveBy
        modes: { accessMode: $accessMode, egressMode: $egressMode }
      ) {
        tripPatterns {
          duration
          legs {
            mode
            duration
            fromPlace { name }
            toPlace { name }
            line { publicCode }
            pointsOnLink { points }
          }
        }
      }
    }
    """;

  private final OtpServerRequestContext serverContext;
  private final TransmodelRequestContext requestContext;
  private final GraphQLSchema schema;
  private final ModeCombinations modeCombinations;

  TransmodelWarmupQueryExecutor(
    OtpServerRequestContext context,
    List<StreetMode> accessModes,
    List<StreetMode> egressModes
  ) {
    this.serverContext = context;
    this.schema = context.transmodelSchema();
    this.requestContext = new TransmodelRequestContext(
      context,
      context.routingService(),
      context.transitService(),
      context.empiricalDelayService()
    );
    this.modeCombinations = new ModeCombinations(accessModes, egressModes);
  }

  @Override
  public boolean execute(WgsCoordinate from, WgsCoordinate to, boolean arriveBy, int queryCount) {
    var variables = buildVariables(from, to, arriveBy, queryCount);

    var input = ExecutionInput.newExecutionInput()
      .query(QUERY)
      .context(requestContext)
      .root(serverContext)
      .variables(variables)
      .build();

    // The AbortOnUnprocessableRequestExecutionStrategy has per-query state
    // (ProgressTracker) and must be created fresh for each execution.
    try (var strategy = new AbortOnUnprocessableRequestExecutionStrategy()) {
      var graphQL = GraphQL.newGraphQL(schema).queryExecutionStrategy(strategy).build();
      var result = graphQL.execute(input);
      if (!result.getErrors().isEmpty()) {
        LOG.warn("Warmup query had GraphQL errors: {}", result.getErrors());
        return false;
      }
      return true;
    }
  }

  private static String toGraphQLName(StreetMode mode) {
    return EnumTypes.STREET_MODE.getValues()
      .stream()
      .filter(v -> v.getValue() == mode)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("No TransModel mapping for " + mode))
      .getName();
  }

  Map<String, Object> buildVariables(
    WgsCoordinate from,
    WgsCoordinate to,
    boolean arriveBy,
    int queryCount
  ) {
    return Map.of(
      "fromLat",
      from.latitude(),
      "fromLon",
      from.longitude(),
      "toLat",
      to.latitude(),
      "toLon",
      to.longitude(),
      "arriveBy",
      arriveBy,
      "accessMode",
      toGraphQLName(modeCombinations.access(queryCount)),
      "egressMode",
      toGraphQLName(modeCombinations.egress(queryCount))
    );
  }
}
