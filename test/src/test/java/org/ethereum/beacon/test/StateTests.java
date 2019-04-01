package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconChainSpecImpl;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.test.runner.StateRunner;
import org.ethereum.beacon.test.type.StateTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.fail;

public class StateTests extends TestUtils {
  private String TESTS_DIR = "state";

  public StateTests() {}

  @Test
  public void testState() {
    Path stateTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDir(
        stateTestsPath,
        StateTest.class,
        testCase -> {
          StateRunner testCaseRunner =
              new StateRunner(
                  testCase,
                  specConstants ->
                      new BeaconChainSpecImpl(
                          specConstants,
                          Hashes::keccak256,
                          SSZObjectHasher.create(Hashes::keccak256)));
          return testCaseRunner.run();
        });
    //    TODO: remove one file test
//        String filename = "sanity-check_small-config_32-vals.yaml";
//        Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, filename);
//        StateTest test = readTest(getResourceFile(testFilePath.toString()), StateTest.class);
//        Optional<String> errors =
//            runAllCasesInTest(
//                test,
//                testCase -> {
//                  StateRunner testCaseRunner =
//                      new StateRunner(
//                          testCase,
//                          specConstants ->
//                              new BeaconChainSpecImpl(
//                                  specConstants,
//                                  Hashes::keccak256,
//                                  SSZObjectHasher.create(Hashes::keccak256)));
//                  return testCaseRunner.run();
//                },
//                StateTest.class);
//        if (errors.isPresent()) {
//          System.out.println(errors.get());
//          fail();
//        }
  }
}
