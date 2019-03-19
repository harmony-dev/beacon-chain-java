package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.test.runner.SszRunner;
import org.ethereum.beacon.test.type.SszTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SszTests extends TestUtils {
  private String TESTS_DIR = "ssz";
  private SpecHelpers specHelpers;

  public SszTests() {
    this.specHelpers =
        new SpecHelpers(
            SpecConstants.DEFAULT, Hashes::keccak256, SSZObjectHasher.create(Hashes::keccak256));
  }

  @Test
  public void testSsz() {
    Path sszTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDir(
        sszTestsPath,
        SszTest.class,
        testCase -> {
          SszRunner testCaseRunner = new SszRunner(testCase, specHelpers);
          return testCaseRunner.run();
        });
  }
}
