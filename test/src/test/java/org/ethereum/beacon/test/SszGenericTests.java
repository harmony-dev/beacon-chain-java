package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.ssz.SszBitlistRunner;
import org.ethereum.beacon.test.runner.ssz.SszBitvectorRunner;
import org.ethereum.beacon.test.runner.ssz.SszBooleanRunner;
import org.ethereum.beacon.test.runner.ssz.SszBasicVectorRunner;
import org.ethereum.beacon.test.runner.ssz.SszContainerRunner;
import org.ethereum.beacon.test.runner.ssz.SszUintsRunner;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SszGenericTests extends TestUtils {
  private Path SUBDIR = Paths.get("general", "phase0", "ssz_generic");

  @Test
  public void testSszGenericUints() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "uints");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszUintsRunner testRunner = new SszUintsRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }

  @Test
  public void testSszGenericBools() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "boolean");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszBooleanRunner testRunner = new SszBooleanRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }

  @Test
  public void testSszGenericBitvector() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "bitvector");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszBitvectorRunner testRunner =
              new SszBitvectorRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }

  @Test
  public void testSszGenericBitlist() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "bitlist");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszBitlistRunner testRunner = new SszBitlistRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }

  @Test
  public void testSszGenericBasicVector() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "basic_vector");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszBasicVectorRunner testRunner = new SszBasicVectorRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }

  @Test
  public void testSszGenericContainers() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "containers");
    runSszGenericTestsInResourceDir(
        testFileDir,
        input -> {
          SszContainerRunner testRunner = new SszContainerRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        Ignored.EMPTY,
        false);
  }
}
