package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.ssz.SszStaticRunner;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ethereum.beacon.test.TestUtils.MAINNET_TESTS;
import static org.ethereum.beacon.test.TestUtils.MINIMAL_TESTS;
import static org.ethereum.beacon.test.TestUtils.runSszStaticTestsInResourceDir;

/** SSZ static tests, with known core container types */
public class SszStaticTests {
  private Path SUBDIR = Paths.get("phase0", "ssz_static");

  @Test
  public void testSszStaticMinimal() {
    runSszStaticTestsInResourceDir(
        MINIMAL_TESTS,
        SUBDIR,
        input -> {
          SszStaticRunner testRunner = new SszStaticRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        TestUtils.Ignored.EMPTY,
        true);
  }

  @Test
  @Ignore("Takes hours on CI")
  public void testSszStaticMainnet() {
    runSszStaticTestsInResourceDir(
        MAINNET_TESTS,
        SUBDIR,
        input -> {
          SszStaticRunner testRunner = new SszStaticRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        TestUtils.Ignored.EMPTY,
        true);
  }
}
