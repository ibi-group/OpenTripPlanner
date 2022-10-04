package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.smoketest.util.GraphQLClient;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Denver can be found.
 */
@Tag("smoke-test")
@Tag("denver")
public class DenverSmokeTest {

  WgsCoordinate southBroadway = new WgsCoordinate(39.7020, -104.9866);
  WgsCoordinate twinLakes = new WgsCoordinate(39.8232, -105.0055);

  @Test
  public void routeFromSouthToNorth() {
    SmokeTest.basicRouteTest(
      southBroadway,
      twinLakes,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "TRAM", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void vehiclePositions() throws JsonProcessingException {
    var json = GraphQLClient.sendGraphQLRequest(
      """
        query {
        	patterns {
        		vehiclePositions {
        			vehicleId
        			lastUpdated
        			trip {
        				id
        				gtfsId
        			}
        			stopRelationship {
        				status
        				stop {
        					name
        				}
        			}
        		}
        	}
        }
                
          """
    );

    var positions = SmokeTest.mapper.treeToValue(json, VehiclePositionResponse.class);

    var vehiclePositions = positions.patterns
      .stream()
      .flatMap(p -> p.vehiclePositions.stream())
      .toList();

    assertFalse(
      vehiclePositions.isEmpty(),
      "Found no patterns that have realtime vehicle positions."
    );
  }

  private record Position(String vehicleId) {}

  private record Pattern(List<Position> vehiclePositions) {}

  private record VehiclePositionResponse(List<Pattern> patterns) {}
}
