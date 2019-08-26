package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.SanityBlocksCase;
import org.ethereum.beacon.test.type.state.SanitySlotsCase;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateSanityTests extends TestUtils {
  private Path SUBDIR = Paths.get("phase0", "sanity");

  @Test
  public void testSanitySlots() {
    Path subDir = Paths.get(SUBDIR.toString(), "slots");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        SanitySlotsCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testSanityBlocks() {
    Path subDir = Paths.get(SUBDIR.toString(), "blocks");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        SanityBlocksCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
