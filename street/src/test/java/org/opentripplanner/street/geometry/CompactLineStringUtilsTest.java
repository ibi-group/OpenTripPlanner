package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.CompactLineStringUtils.STRAIGHT_LINE_PACKED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

class CompactLineStringUtilsTest {

  private static final GeometryFactory GF = new GeometryFactory();
  private static final double X_0 = 1.111111111;
  private static final double Y_0 = 0.123456789;
  private static final double X_1 = 2.0;
  private static final double Y_1 = 0.0;
  private static final double TOLERANCE = 0.00000015;

  @Test
  void lineStringWithTwoCoordinates() {
    List<Coordinate> c = new ArrayList<>();
    c.add(new Coordinate(X_0, Y_0));
    c.add(new Coordinate(X_1, Y_1));
    LineString ls = GF.createLineString(c.toArray(new Coordinate[0]));
    byte[] coords = CompactLineStringUtils.compactLineString(X_0, Y_0, X_1, Y_1, ls, false);
    // ==, not equals
    assertSame(STRAIGHT_LINE_PACKED, coords);
    LineString ls2 = CompactLineStringUtils.uncompactLineString(X_0, Y_0, X_1, Y_1, coords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));
    byte[] packedCoords = CompactLineStringUtils.compactLineString(X_0, Y_0, X_1, Y_1, ls, false);
    // ==, not equals
    assertSame(STRAIGHT_LINE_PACKED, packedCoords);
    ls2 = CompactLineStringUtils.uncompactLineString(X_0, Y_0, X_1, Y_1, packedCoords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));
  }

  @Test
  void roundTrip() {
    List<Coordinate> c = new ArrayList<>();
    c.add(new Coordinate(X_0, Y_0));
    c.add(new Coordinate(-179.99, 1.12345));
    c.add(new Coordinate(179.99, 1.12345));
    c.add(new Coordinate(X_1, Y_1));
    var ls = GF.createLineString(c.toArray(new Coordinate[0]));
    var coords = CompactLineStringUtils.compactLineString(X_0, Y_0, X_1, Y_1, ls, false);
    assertNotSame(STRAIGHT_LINE_PACKED, coords);
    var ls2 = CompactLineStringUtils.uncompactLineString(X_0, Y_0, X_1, Y_1, coords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));
    var packedCoords = CompactLineStringUtils.compactLineString(X_0, Y_0, X_1, Y_1, ls, false);
    assertNotSame(STRAIGHT_LINE_PACKED, packedCoords);
    ls2 = CompactLineStringUtils.uncompactLineString(X_0, Y_0, X_1, Y_1, packedCoords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));

    // Test reverse mode

    // The expected output
    LineString lsi = ls.reverse();
    byte[] coords2 = CompactLineStringUtils.compactLineString(X_1, Y_1, X_0, Y_0, ls, true);
    assertNotSame(STRAIGHT_LINE_PACKED, coords2);
    assertEquals(coords.length, coords2.length);
    for (int i = 0; i < coords.length; i++) {
      assertEquals(coords[i], coords2[i]);
    }
    ls2 = CompactLineStringUtils.uncompactLineString(X_1, Y_1, X_0, Y_0, coords2, true);
    assertTrue(lsi.equalsExact(ls2, TOLERANCE));
    LineString ls3 = CompactLineStringUtils.uncompactLineString(X_1, Y_1, X_0, Y_0, coords, true);
    assertTrue(lsi.equalsExact(ls3, TOLERANCE));
    byte[] packedCoords2 = CompactLineStringUtils.compactLineString(X_1, Y_1, X_0, Y_0, ls, true);
    assertNotSame(STRAIGHT_LINE_PACKED, packedCoords2);
    assertEquals(packedCoords.length, packedCoords2.length);
    for (int i = 0; i < packedCoords.length; i++) {
      assertEquals(packedCoords[i], packedCoords2[i]);
    }
    ls2 = CompactLineStringUtils.uncompactLineString(X_1, Y_1, X_0, Y_0, packedCoords2, true);
    assertTrue(lsi.equalsExact(ls2, TOLERANCE));
    ls3 = CompactLineStringUtils.uncompactLineString(X_1, Y_1, X_0, Y_0, packedCoords, true);
    assertTrue(lsi.equalsExact(ls3, TOLERANCE));
  }

  @Test
  void testNoEndpointRoundTrip() {
    Coordinate[] points = {
      new Coordinate(-179.99, 1.12345),
      new Coordinate(10.123456, 59.654321),
      new Coordinate(179.99, 1.12345),
    };
    LineString original = GF.createLineString(points);

    // Forward
    byte[] packed = CompactLineStringUtils.compactLineString(original, false);
    LineString result = CompactLineStringUtils.uncompactLineString(packed, false);
    assertEquals(original.getNumPoints(), result.getNumPoints());
    assertTrue(original.equalsExact(result, 0.0000015));

    // Reverse
    LineString reversed = original.reverse();
    result = CompactLineStringUtils.uncompactLineString(packed, true);
    assertEquals(reversed.getNumPoints(), result.getNumPoints());
    assertTrue(reversed.equalsExact(result, 0.0000015));
  }

  @Test
  void testDlugoszVarLenIntPacker() {
    packTest(new int[] {}, 0);
    packTest(new int[] { 0 }, 1);
    packTest(new int[] { 63 }, 1);
    packTest(new int[] { -64 }, 1);
    packTest(new int[] { 64 }, 2);
    packTest(new int[] { -65 }, 2);
    packTest(new int[] { -8192 }, 2);
    packTest(new int[] { -8193 }, 3);
    packTest(new int[] { 8191 }, 2);
    packTest(new int[] { 8192 }, 3);
    packTest(new int[] { -1048576 }, 3);
    packTest(new int[] { -1048577 }, 4);
    packTest(new int[] { 1048575 }, 3);
    packTest(new int[] { 1048576 }, 4);
    packTest(new int[] { -67108864 }, 4);
    packTest(new int[] { -67108865 }, 5);
    packTest(new int[] { 67108863 }, 4);
    packTest(new int[] { 67108864 }, 5);
    packTest(new int[] { Integer.MAX_VALUE }, 5);
    packTest(new int[] { Integer.MIN_VALUE }, 5);

    packTest(new int[] { 0, 0 }, 2);
    packTest(new int[] { 0, 0, 0 }, 3);

    packTest(new int[] { 8100, 8200, 8300 }, 8);
  }

  private void packTest(int[] arr, int expectedPackedLen) {
    byte[] packed = DlugoszVarLenIntPacker.pack(arr);
    System.out.println(
      "Unpacked: " + Arrays.toString(arr) + " -> packed: " + unsignedCharString(packed)
    );
    assertEquals(expectedPackedLen, packed.length);
    int[] unpacked = DlugoszVarLenIntPacker.unpack(packed);
    assertArrayEquals(arr, unpacked);
  }

  private String unsignedCharString(byte[] data) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < data.length; i++) {
      sb.append(String.format("%02X", data[i] & 0xFF));
      if (i < data.length - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
