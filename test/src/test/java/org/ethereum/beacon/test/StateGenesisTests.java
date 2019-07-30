package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.GenesisInitRunner;
import org.ethereum.beacon.test.runner.state.GenesisValidityRunner;
import org.ethereum.beacon.test.type.state.GenesisInitTest;
import org.ethereum.beacon.test.type.state.GenesisValidityTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateGenesisTests extends TestUtils {

  private String SUBDIR = "genesis";

  @Test
  public void testGenesisInitialization() {
    final String type = "initialization";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        GenesisInitTest.class,
        input -> {
          GenesisInitRunner testRunner =
              new GenesisInitRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }

  @Test
  public void testGenesisValidity() {
    final String type = "validity";
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR, type);
    runTestsInResourceDir(
        testFileDir,
        GenesisValidityTest.class,
        input -> {
          GenesisValidityRunner testRunner =
              new GenesisValidityRunner(input.getValue0(), input.getValue1(), type);
          return testRunner.run();
        });
  }
}
