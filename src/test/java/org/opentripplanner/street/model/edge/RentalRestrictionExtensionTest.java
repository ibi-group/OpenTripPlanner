package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.street.model.vertex.RentalRestrictionExtension;
import org.opentripplanner.street.model.vertex.RentalRestrictionExtension.BusinessAreaBorder;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RentalRestrictionExtensionTest {

  String network = "tier-oslo";
  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  StreetVertex V3 = intersectionVertex("V3", 2, 2);
  StreetVertex V4 = intersectionVertex("V4", 3, 3);

  @Test
  public void leaveBusinessAreaOnFoot() {
    var edge1 = streetEdge(V1, V2);
    var ext = new BusinessAreaBorder(network);
    V2.addRentalRestriction(ext);

    State result = traverse(edge1);
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
    assertEquals(TraverseMode.WALK, result.getBackMode());
    assertNull(result.getNextResult());
  }

  @Test
  public void dontEnterGeofencingZoneOnFoot() {
    var edge = streetEdge(V1, V2);
    V2.addRentalRestriction(
      new RentalRestrictionExtension.GeofencingZoneExtension(
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
    restrictedEdge.addRentalRestriction(
      new RentalRestrictionExtension.GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
      )
    );

    var continueOnFoot = edge1.traverse(editor.makeState());

    assertEquals(HAVE_RENTED, continueOnFoot.getVehicleRentalState());
    assertEquals(WALK, continueOnFoot.getBackMode());

    var continueRenting = continueOnFoot.getNextResult();
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
    assertEquals(BICYCLE, continueRenting.getBackMode());
    assertTrue(continueRenting.isInsideNoRentalDropOffArea());

    var insideZone = restrictedEdge.traverse(continueRenting);

    var leftNoDropOff = edge2.traverse(insideZone);
    assertFalse(leftNoDropOff.isInsideNoRentalDropOffArea());
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
  }

  @Test
  public void dontFinishInNoDropOffZone() {
    var edge = streetEdge(V1, V2);
    var ext = new RentalRestrictionExtension.GeofencingZoneExtension(
      new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
    );
    V2.addRentalRestriction(ext);
    edge.addRentalRestriction(ext);
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
    edge.addRentalRestriction(new BusinessAreaBorder("a"));
    edge.addRentalRestriction(new BusinessAreaBorder("b"));

    assertTrue(edge.fromv.rentalTraversalBanned(state("a")));
    assertTrue(edge.fromv.rentalTraversalBanned(state("b")));
  }

  @Test
  public void removeExtensions() {
    var edge = streetEdge(V1, V2);
    var a = new BusinessAreaBorder("a");
    var b = new BusinessAreaBorder("b");
    var c = new BusinessAreaBorder("c");

    edge.addRentalRestriction(a);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("a")));

    edge.addRentalRestriction(b);
    edge.addRentalRestriction(c);

    edge.removeRentalExtension(a);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("b")));
    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("c")));

    edge.removeRentalExtension(b);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("c")));
  }

  @Test
  public void checkNetwork() {
    var edge = streetEdge(V1, V2);
    edge.addRentalRestriction(new BusinessAreaBorder("a"));

    var state = traverse(edge);

    assertEquals(RENTING_FLOATING, state.getVehicleRentalState());
    assertNull(state.getNextResult());
  }

  private State traverse(StreetEdge edge) {
    var state = state(network);
    return edge.traverse(state);
  }

  @Nonnull
  private State state(String network) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(V1, req);
    editor.beginFloatingVehicleRenting(RentalVehicleType.FormFactor.SCOOTER, network, false);
    return editor.makeState();
  }

  @Nested
  class Composition {

    RentalRestrictionExtension a = new BusinessAreaBorder("a");
    RentalRestrictionExtension b = new BusinessAreaBorder("b");
    RentalRestrictionExtension c = new RentalRestrictionExtension.GeofencingZoneExtension(
      new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
    );

    @Test
    void addToBase() {
      var newA = RentalRestrictionExtension.NO_RESTRICTION.add(a);
      assertSame(a, newA);
      assertEquals(1, newA.toList().size());
    }

    @Test
    void addToItself() {
      var unchanged = a.add(a);
      assertSame(a, unchanged);
    }

    @Test
    void add() {
      var composite = a.add(b);
      assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
    }

    @Test
    void differentType() {
      var composite = a.add(c);
      assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
    }

    @Test
    void composite() {
      var composite = a.add(b);
      assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
      var newComposite = composite.add(c);
      assertInstanceOf(RentalRestrictionExtension.Composite.class, newComposite);

      var c1 = (RentalRestrictionExtension.Composite) newComposite;
      var exts = c1.toList();
      assertEquals(3, exts.size());

      var c2 = (RentalRestrictionExtension.Composite) c1.add(a);
      assertEquals(3, c2.toList().size());
      // convert to sets so the order doesn't matter
      assertEquals(Set.of(a, b, c), Set.copyOf(c2.toList()));
    }
  }
}
