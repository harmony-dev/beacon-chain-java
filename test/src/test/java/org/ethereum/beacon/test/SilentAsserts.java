package org.ethereum.beacon.test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Provides set of methods which wraps asserts and returns {@link java.util.Optional} assert message
 */
public class SilentAsserts {
  public static Optional<String> assertLists(List expected, List actual) {
    try {
      assertArrayEquals(expected.toArray(), actual.toArray());
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }

  public static Optional<String> assertHexStrings(String expected, String actual) {
    String expectedHex = expected.startsWith("0x") ? expected : "0x" + expected;
    String actualHex = actual.startsWith("0x") ? actual : "0x" + actual;
    try {
      assertEquals(expectedHex, actualHex);
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }
}
