package org.ethereum.beacon.test;

import org.junit.Assert;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;

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
      Assert.assertEquals(expectedHex, actualHex);
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }

  public static Optional<String> assertEquals(Object expected, Object actual) {
    try {
      Assert.assertEquals(expected, actual);
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }

  public static Optional<String> assertTrue(String msg, boolean value) {
    try {
      Assert.assertTrue(msg, value);
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }
}
