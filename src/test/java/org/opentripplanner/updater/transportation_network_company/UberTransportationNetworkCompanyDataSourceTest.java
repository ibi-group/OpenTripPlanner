/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.updater.transportation_network_company.uber.UberAuthenticationRequestBody;
import org.opentripplanner.updater.transportation_network_company.uber.UberTransportationNetworkCompanyDataSource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UberTransportationNetworkCompanyDataSourceTest {

    public static final String CLIENT_ID = "testClientId";
    public static final String CLIENT_SECRET = "testClientSecret";
    public static final String INVALID_CLIENT_ID = "invalidClientId";
    public static final String TOKEN_PATH = "/login/oauth/v2/token";
    public static final String API_TIME_PATH = "/api/estimates/time";
    public static final String API_PRICE_PATH = "/api/estimates/price";
    public static final String MOCK_API_URL = "http://localhost:8089/api/";
    public static final String MOCK_TOKEN_URL = "http://localhost:8089/login/";

    private static final UberTransportationNetworkCompanyDataSource source = new UberTransportationNetworkCompanyDataSource(
        MOCK_API_URL,
        MOCK_TOKEN_URL,
        CLIENT_ID,
        CLIENT_SECRET
    );

    private static final UUID TOKEN_STUB_ID = UUID.randomUUID();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
        options()
            .port(8089)
            .usingFilesUnderDirectory("src/test/resources/updater/")
    );

    // Copied from lyft code (TODO: refactor)
    @Before
    public void setUp() throws Exception {
        String accessToken = UUID.randomUUID().toString().replaceAll("-", "");
        // setup mock server to respond to ride estimate request
        stubFor(
            post(urlPathEqualTo(TOKEN_PATH))
                .withId(TOKEN_STUB_ID)
                .withRequestBody(equalTo(
                    new UberAuthenticationRequestBody(
                        CLIENT_ID,
                        CLIENT_SECRET
                    ).toRequestParamString()
                ))
                .willReturn(
                    aResponse()
                        .withBody(getAuthorizationResponseBody(accessToken))
                )
        );
        stubFor(
            post(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(equalTo(
                    new UberAuthenticationRequestBody(
                        INVALID_CLIENT_ID,
                        CLIENT_SECRET
                    ).toRequestParamString()
                ))
                .willReturn(
                    aResponse()
                        .withBody("{\"error\":\"unauthorized_client\"}")
                )
        );
    }

    private String getAuthorizationResponseBody(String accessToken) throws JsonProcessingException {
        OAuthAuthenticationResponse response = new OAuthAuthenticationResponse();
        response.access_token = accessToken;
        response.expires_in = 2592000;
        response.scope = "ride_request.estimate";
        response.token_type = "Bearer";
        return new ObjectMapper().writeValueAsString(response);
    }

    @Test
    public void testGetAccessTokenSuccess() throws IOException {
        OAuthToken initialToken = source.getToken();
        initialToken.makeTokenExpire();
        assertTrue(initialToken.isInvalid());
        // Get access token for the first time.
        String token1 = source.getToken().value;

        // Edit stub so that it returns a different token when called.
        // (Modified stub doesn't need to be reset as we don't rely on a particular token value.)
        String newAccessToken = UUID.randomUUID().toString().replaceAll("-", "");
        editStub(get(urlPathEqualTo(TOKEN_PATH))
            .withId(TOKEN_STUB_ID)
            .willReturn(
                aResponse()
                    .withBody(getAuthorizationResponseBody(newAccessToken))));

        // Second call to getAccessToken should return the same original token, as it has not expired yet.
        OAuthToken newToken = source.getToken();
        assertFalse(newToken.isInvalid());
        assertEquals(token1, newToken.value);
    }

    @Test
    public void testGetAccessTokenError() throws IOException {
        UberTransportationNetworkCompanyDataSource src = new UberTransportationNetworkCompanyDataSource(
            MOCK_API_URL,
            MOCK_TOKEN_URL,
            INVALID_CLIENT_ID,
            CLIENT_SECRET
        );

        // Token is null, so we should still attempt to get one.
        OAuthToken token = src.getToken();
        assertNull(token.value);
        assertTrue(token.isInvalid());
    }

    @Test
    public void testGetArrivalTimes() throws ExecutionException {
        // Setup mock server to respond to ride estimate request.
        // The mock server expects a bearer token in the header per Uber API requirements as of 2022.
        stubFor(
            get(urlPathEqualTo(API_TIME_PATH))
                .withHeader("Authorization", matching("^Bearer [\\w|\\.]*"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .willReturn(
                    aResponse()
                        .withBodyFile("uber_arrival_estimates.json")
                )
        );
        // Old server tokens will be rejected at some point, so treat them the same as unauthorized.
        stubFor(
            get(urlPathEqualTo(API_TIME_PATH))
                .withHeader("Authorization", matching("^Token [\\w|\\.]*"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .willReturn(unauthorized())
        );
        // If no authentication is passed, I guess the API would return unauthorized.
        stubFor(
            get(urlPathEqualTo(API_TIME_PATH))
                .withHeader("Authorization", StringValuePattern.ABSENT)
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .willReturn(unauthorized())
        );


        List<ArrivalTime> arrivalTimes = source.getArrivalTimes(1.2, 3.4);

        assertEquals(8, arrivalTimes.size());
        ArrivalTime arrival = arrivalTimes.get(0);
        assertEquals("POOL", arrival.displayName);
        assertEquals("26546650-e557-4a7b-86e7-6a3942445247", arrival.productId);
        assertEquals(60, arrival.estimatedSeconds);
    }

    @Test
    public void testGetEstimatedRideTimeAndCost() throws ExecutionException {
        // Setup mock server to respond to estimated ride time request.
        // The mock server expects a bearer token in the header per Uber API requirements as of 2022.
        stubFor(
            get(urlPathEqualTo(API_PRICE_PATH))
                .withHeader("Authorization", matching("^Bearer [\\w|\\.]*"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .withQueryParam("end_latitude", equalTo("1.201"))
                .withQueryParam("end_longitude", equalTo("3.401"))
                .willReturn(
                    aResponse()
                        .withBodyFile("uber_trip_estimates.json")
                )
        );
        // Old server tokens will be rejected at some point, so treat them the same as unauthorized.
        stubFor(
            get(urlPathEqualTo(API_PRICE_PATH))
                .withHeader("Authorization", matching("^Token [\\w|\\.]*"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .withQueryParam("end_latitude", equalTo("1.201"))
                .withQueryParam("end_longitude", equalTo("3.401"))
                .willReturn(unauthorized())
        );
        // If no authentication is passed, I guess the API would return unauthorized.
        stubFor(
            get(urlPathEqualTo(API_PRICE_PATH))
                .withHeader("Authorization", StringValuePattern.ABSENT)
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .withQueryParam("end_latitude", equalTo("1.201"))
                .withQueryParam("end_longitude", equalTo("3.401"))
                .willReturn(unauthorized())
        );

        List<RideEstimate> rideEstimates = source.getRideEstimates(
            1.2,
            3.4,
            1.201,
            3.401
        );

        RideEstimate rideEstimate = null;

        for (RideEstimate estimate : rideEstimates) {
            if (estimate.rideType.equals("a1111c8c-c720-46c3-8534-2fcdd730040d")) {
                rideEstimate = estimate;
                break;
            }
        }

        assertNotNull(rideEstimate);
        assertEquals(1080, rideEstimate.duration);
        assertEquals(13, rideEstimate.minCost, 0.001);
        assertEquals(17, rideEstimate.maxCost, 0.001);
    }
}
