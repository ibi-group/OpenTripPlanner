package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.V1;
import static org.opentripplanner.street.model._data.StreetModelForTest.V2;
import static org.opentripplanner.street.model._data.StreetModelForTest.V3;
import static org.opentripplanner.street.model._data.StreetModelForTest.V4;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.BusinessAreaBorder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class StreetEdgeRentalExtensionTest {

  String network = "tier-oslo";

  @Test
  public void leaveBusinessAreaOnFoot() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(new BusinessAreaBorder(network));
    State result = traverse(edge);
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
    assertEquals(TraverseMode.WALK, result.getBackMode());
    assertNull(result.getNextResult());
  }

  @Test
  public void dontEnterGeofencingZoneOnFoot() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(
      new StreetEdgeRentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, true)
      )
    );
    State result = traverse(edge);
    assertEquals(WALK, result.getBackMode());
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
  }

  @Test
  public void forkStateWhenEnteringNoDropOffZone() {
    var edge1 = streetEdge(V4, V1);
    var edge2 = streetEdge(V2, V3);
    var restrictedEdge = streetEdge(V1, V2);

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(edge1.getFromVertex(), req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    restrictedEdge.addRentalExtension(
      new StreetEdgeRentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );

    var isRenting = edge1.traverse(editor.makeState());
    var continueOnFoot = restrictedEdge.traverse(isRenting);
    assertEquals(HAVE_RENTED, continueOnFoot.getVehicleRentalState());
    assertEquals(WALK, continueOnFoot.getBackMode());

    var continueRenting = continueOnFoot.getNextResult();
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
    assertEquals(BICYCLE, continueRenting.getBackMode());
    assertTrue(continueRenting.isInsideNoRentalDropOffArea());

    var leftNoDropOff = edge2.traverse(continueRenting);
    assertFalse(leftNoDropOff.isInsideNoRentalDropOffArea());
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
  }

  @Test
  public void dontFinishInNoDropOffZone() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(
      new StreetEdgeRentalExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );
    State result = traverse(edge);
    assertFalse(result.isFinal());
  }

  @Test
  public void finishInEdgeWithoutRestrictions() {
    var edge = streetEdge(V1, V2);
    State result = traverse(edge);
    assertTrue(result.isFinal());
  }

  @Test
  public void addTwoExtensions() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(new BusinessAreaBorder("a"));
    edge.addRentalExtension(new BusinessAreaBorder("b"));

    assertEquals(2, edge.getTraversalExtensions().size());
  }

  @Test
  public void removeExtensions() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(new BusinessAreaBorder("a"));
    edge.addRentalExtension(new BusinessAreaBorder("b"));
    edge.addRentalExtension(new BusinessAreaBorder("b"));

    edge.removeTraversalExtension("a");
    assertEquals(2, edge.getTraversalExtensions().size());

    edge.removeTraversalExtension("b");
    assertEquals(0, edge.getTraversalExtensions().size());
  }

  @Test
  public void checkNetwork() {
    var edge = streetEdge(V1, V2);
    edge.addRentalExtension(new BusinessAreaBorder("a"));

    var state = traverse(edge);

    assertEquals(RENTING_FLOATING, state.getVehicleRentalState());
    assertNull(state.getNextResult());
  }

  private State traverse(StreetEdge edge) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(V1, req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    return edge.traverse(editor.makeState());
  }
}
