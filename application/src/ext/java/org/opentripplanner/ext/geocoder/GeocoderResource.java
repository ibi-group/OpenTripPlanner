package org.opentripplanner.ext.geocoder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * OTP simple built-in geocoder used by the debug client.
 */
@Path("/geocode")
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderResource {

  private final LuceneIndex luceneIndex;

  public GeocoderResource(@Context OtpServerRequestContext requestContext) {
    luceneIndex = requestContext.lucenceIndex();
  }

  @GET
  @Path("stopClusters")
  public Response stopClusters(@QueryParam("query") String query) {
    var clusters = luceneIndex.queryStopClusters(query).toList();

    return Response.status(Response.Status.OK).entity(clusters).build();
  }
}
