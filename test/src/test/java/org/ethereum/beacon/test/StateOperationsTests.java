package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.OperationAttestationCase;
import org.ethereum.beacon.test.type.state.OperationAttesterSlashingCase;
import org.ethereum.beacon.test.type.state.OperationBlockHeaderCase;
import org.ethereum.beacon.test.type.state.OperationDepositCase;
import org.ethereum.beacon.test.type.state.OperationProposerSlashingCase;
import org.ethereum.beacon.test.type.state.OperationTransferCase;
import org.ethereum.beacon.test.type.state.OperationVoluntaryExitCase;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateOperationsTests extends TestUtils {
  private Path SUBDIR = Paths.get("phase0", "operations");

  @Test
  public void testAttestationOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "attestation");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationAttestationCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testTransferOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "transfer");
    // No mainnet tests for `transfer`s
    runSpecTestsInResourceDir(
        MINIMAL_TESTS,
        subDir,
        OperationTransferCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testDepositOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "deposit");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationDepositCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testAttesterSlashingOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "attester_slashing");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationAttesterSlashingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testProposerSlashingOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "proposer_slashing");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationProposerSlashingCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testVoluntaryExitOperations() {
    Path subDir = Paths.get(SUBDIR.toString(), "voluntary_exit");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationVoluntaryExitCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlockProcessing() {
    Path subDir = Paths.get(SUBDIR.toString(), "block_header");
    runSpecTestsInResourceDirs(
        MINIMAL_TESTS,
        MAINNET_TESTS,
        subDir,
        OperationBlockHeaderCase.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
