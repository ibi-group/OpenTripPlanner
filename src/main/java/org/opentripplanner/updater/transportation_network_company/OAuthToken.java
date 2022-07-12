package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Date;

/**
 * Holds an OAuth access token for querying ride-hail APIs.
 */
public class OAuthToken {
    public final String value;
    private Date tokenExpirationTime;

    public OAuthToken() {
        value = null;
    }

    public OAuthToken(HttpURLConnection connection) throws IOException {
        // send request and parse response
        ObjectMapper mapper = new ObjectMapper();
        InputStream responseStream = connection.getInputStream();
        OAuthAuthenticationResponse response = mapper.readValue(responseStream, OAuthAuthenticationResponse.class);
        responseStream.close();

        value = response.access_token;
        tokenExpirationTime = new Date();
        tokenExpirationTime.setTime(tokenExpirationTime.getTime() + (response.expires_in - 60) * 1000L);
    }


    /**
     * Checks if a new token needs to be obtained.
     */
    public boolean shouldRenew() {
        return tokenExpirationTime == null || new Date().after(tokenExpirationTime);
    }

    /**
     * Used for testing purposes only
     */
    public void makeTokenExpire() {
        tokenExpirationTime = Date.from(Instant.now().minusSeconds(1));
    }
}
