package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.runner.SszRunner;
import org.ethereum.beacon.test.type.SszTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SszTests extends TestUtils {
  private String TESTS_DIR = "ssz";
  private SpecHelpers specHelpers;

  public SszTests() {
    this.specHelpers = SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT);
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
