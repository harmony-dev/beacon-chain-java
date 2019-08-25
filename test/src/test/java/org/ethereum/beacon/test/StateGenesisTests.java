package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.state.GenesisInitRunner;
import org.ethereum.beacon.test.runner.state.GenesisValidityRunner;
import org.ethereum.beacon.test.type.state.GenesisInitCase;
import org.ethereum.beacon.test.type.state.GenesisValidityCase;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StateGenesisTests extends TestUtils {
  private Path SUBDIR = Paths.get("phase0", "genesis");

  @Test
  public void testGenesisInitialization() {
    Path subDir = Paths.get(SUBDIR.toString(), "initialization");
    runSpecTestsInResourceDir(
        MINIMAL_TESTS,
        subDir,
        GenesisInitCase.class,
        input -> {
          GenesisInitRunner testRunner =
              new GenesisInitRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testGenesisValidity() {
    Path subDir = Paths.get(SUBDIR.toString(), "validity");
    runSpecTestsInResourceDir(
        MINIMAL_TESTS,
        subDir,
        GenesisValidityCase.class,
        input -> {
          GenesisValidityRunner testRunner =
              new GenesisValidityRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
