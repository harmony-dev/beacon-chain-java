package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.StateTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateOperationsTests extends TestUtils {

  private String SUBDIR = "operations";

  @Test
  public void testAttestationOperations() {
    final String type = "attestation";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        },
        Ignored.filesOf("attestation_mainnet.yaml"));
  }

  @Test
  public void testAttesterSlashingOperations() {
    final String type = "attester_slashing";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        },
        Ignored.filesOf("attester_slashing_mainnet.yaml"));
  }

  @Test
  public void testProposerSlashingOperations() {
    final String type = "proposer_slashing";
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
  public void testTransferOperations() {
    final String type = "transfer";
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
  public void testVoluntaryExitOperations() {
    final String type = "voluntary_exit";
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
  public void testBlockProcessing() {
    final String type = "block_header";
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
  public void testDepositOperations() {
    final String type = "deposit";
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
