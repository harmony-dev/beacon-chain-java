package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.StateRunner;
import org.ethereum.beacon.test.type.state.StateTest;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StateSanityTests extends TestUtils {

  private String SUBDIR = "sanity";

  @Test
  public void testSanitySlots() {
    final String type = "slots";
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
  public void testSanityBlocksMin() {
    final String type = "blocks";
    File testFile =
        getResourceFile(
            Paths.get(PATH_TO_TESTS, SUBDIR, type, "sanity_blocks_minimal.yaml").toString());
    runAllTestsInFile(
        testFile,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        },
        StateTest.class);
  }

  @Test
  @Ignore("Requires a lot of memory")
  public void testSanityBlocksMain() {
    final String type = "blocks";
    File testFile =
        getResourceFile(
            (Paths.get(PATH_TO_TESTS, SUBDIR, type, "sanity_blocks_mainnet.yaml").toString()));
    runAllTestsInFile(
        testFile,
        input -> {
          StateRunner testRunner = new StateRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        },
        StateTest.class);
  }
}
