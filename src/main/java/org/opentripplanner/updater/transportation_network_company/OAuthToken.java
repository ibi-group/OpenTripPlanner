package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Date;

/**
 * Holds an OAuth access token and its expiration time for querying ride-hail APIs.
 */
public class OAuthToken {
    public final String value;
    private Date tokenExpirationTime;

    private OAuthToken() {
        value = null;
    }

    public static OAuthToken blank() {
        return new OAuthToken();
    }

    public OAuthToken(HttpURLConnection connection) throws IOException {
        // send request and parse response
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream responseStream = connection.getInputStream()) {
            OAuthAuthenticationResponse response = mapper.readValue(responseStream, OAuthAuthenticationResponse.class);

            value = response.access_token;
            tokenExpirationTime = new Date();
            tokenExpirationTime.setTime(tokenExpirationTime.getTime() + (response.expires_in - 60) * 1000L);
        }
    }

    /**
     * Checks if a new token needs to be obtained.
     */
    public boolean isExpired() {
        return tokenExpirationTime == null || new Date().after(tokenExpirationTime);
    }

    /**
     * Used for testing purposes only.
     */
    public void makeTokenExpire() {
        tokenExpirationTime = Date.from(Instant.now().minusSeconds(1));
    }
}
