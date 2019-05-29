package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.StateTest;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateTests extends TestUtils {
  private String OPERATIONS_TESTS_DIR = "operations";

  @Test
  public void testAttestationOperations() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, "attestation");
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testDepositOperations() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, "deposit");
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
