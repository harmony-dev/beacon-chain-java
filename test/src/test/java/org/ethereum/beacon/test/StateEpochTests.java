package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.FinalUpdatesProcessingCase;
import org.ethereum.beacon.test.type.state.FinalizationProcessingCase;
import org.ethereum.beacon.test.type.state.RegistryUpdatesProcessingCase;
import org.ethereum.beacon.test.type.state.SlashingsProcessingCase;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateEpochTests extends TestUtils {
  private Path SUBDIR = Paths.get("phase0", "epoch_processing");

  @Test
  public void testRegistryUpdates() {
    Path subDir = Paths.get(SUBDIR.toString(), "registry_updates");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        RegistryUpdatesProcessingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testFinalUpdates() {
    Path subDir = Paths.get(SUBDIR.toString(), "final_updates");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        FinalUpdatesProcessingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testJustificationAndFinalization() {
    Path subDir = Paths.get(SUBDIR.toString(), "justification_and_finalization");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        FinalizationProcessingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testSlashings() {
    Path subDir = Paths.get(SUBDIR.toString(), "slashings");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        SlashingsProcessingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
