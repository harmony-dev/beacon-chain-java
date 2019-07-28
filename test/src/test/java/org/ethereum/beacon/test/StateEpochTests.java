package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.StateTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateEpochTests extends TestUtils {

  private String SUBDIR = "epoch_processing";

  @Test
  public void testCrosslinksProcessing() {
    final String type = "crosslinks";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testRegistryUpdates() {
    final String type = "registry_updates";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testFinalUpdates() {
    final String type = "final_updates";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testJustificationAndFinalization() {
    final String type = "justification_and_finalization";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        },
        Ignored.filesOf("justification_and_finalization_mainnet.yaml"));
  }

  @Test
  public void testSlashings() {
    final String type = "slashings";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }
}
