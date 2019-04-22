package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.test.runner.ssz.SszRunner;
import org.ethereum.beacon.test.type.ssz.SszTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SszTests extends TestUtils {
  private String TESTS_DIR = "ssz";
  private BeaconChainSpec spec;

  public SszTests() {
    this.spec = BeaconChainSpec.createWithDefaults();
  }

  @Test
  public void testSsz() {
    Path sszTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDir(
        sszTestsPath,
        SszTest.class,
        testCase -> {
          SszRunner testCaseRunner = new SszRunner(testCase, spec);
          return testCaseRunner.run();
        });
  }
}
