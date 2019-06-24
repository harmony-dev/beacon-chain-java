package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.StateTest;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateTests extends TestUtils {
  private String OPERATIONS_TESTS_DIR = "operations";
  private String EPOCH_PROCESSING_DIR = "epoch_processing";
  private String SANITY_PROCESSING_DIR = "sanity";

  @Test
  @Ignore("Requires fixtures update")
  public void testAttestationOperations() {
    final String type = "attestation";
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testAttesterSlashingOperations() {
    final String type = "attester_slashing";
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testProposerSlashingOperations() {
    final String type = "proposer_slashing";
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
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
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
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
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
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
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
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
    Path testFileDir = Paths.get(PATH_TO_TESTS, OPERATIONS_TESTS_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testCrosslinksProcessing() {
    final String type = "crosslinks";
    Path testFileDir = Paths.get(PATH_TO_TESTS, EPOCH_PROCESSING_DIR, type);
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
    Path testFileDir = Paths.get(PATH_TO_TESTS, EPOCH_PROCESSING_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  @Ignore("Requires fixtures update")
  public void testSanitySlots() {
    final String type = "slots";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SANITY_PROCESSING_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  @Ignore("Requires fixtures update")
  public void testSanityBlocks() {
    final String type = "blocks";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SANITY_PROCESSING_DIR, type);
    runTestsInResourceDir(
        testFileDir,
        StateTest.class,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }
}
