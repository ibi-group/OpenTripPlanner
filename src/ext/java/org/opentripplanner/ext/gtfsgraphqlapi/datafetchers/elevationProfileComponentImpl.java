package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.model.plan.ElevationProfile;

public class elevationProfileComponentImpl
  implements GraphQLDataFetchers.GraphQLElevationProfileComponent {

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).x();
  }

  @Override
  public DataFetcher<Double> elevation() {
    return environment -> getSource(environment).y();
  }

  private ElevationProfile.Step getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
