package org.opentripplanner.framework.io;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.ContentType;

public final class HttpUtils {

  private HttpUtils() {}

  public static final Object TEXT_PLAIN = ContentType.create("text/plain", StandardCharsets.UTF_8);
  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";

  private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
  private static final String HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";
  private static final String HEADER_HOST = "Host";

  /**
   * Get the canonical url of a request, either based on headers or the URI. See
   * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host">here</a>
   * for details
   */
  public static String getBaseAddress(UriInfo uri, HttpHeaders headers) {
    String protocol;
    if (headers.getRequestHeader(HEADER_X_FORWARDED_PROTO) != null) {
      protocol = headers.getRequestHeader(HEADER_X_FORWARDED_PROTO).get(0);
    } else {
      protocol = uri.getRequestUri().getScheme();
    }

    String host;
    if (headers.getRequestHeader(HEADER_X_FORWARDED_HOST) != null) {
      host = headers.getRequestHeader(HEADER_X_FORWARDED_HOST).get(0);
    } else if (headers.getRequestHeader(HEADER_HOST) != null) {
      host = headers.getRequestHeader(HEADER_HOST).get(0);
    } else {
      host = uri.getBaseUri().getHost() + ":" + uri.getBaseUri().getPort();
    }

    return protocol + "://" + host;
  }
}
