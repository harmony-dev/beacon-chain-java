package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.ssz.SszBitlistRunner;
import org.ethereum.beacon.test.runner.ssz.SszBitvectorRunner;
import org.ethereum.beacon.test.runner.ssz.SszBooleanRunner;
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
        Ignored.filesOf( // Incorrect cases: 0-filled bits in excess of size. Doesn't matter for 0's
            // until overflow full bytes
            "bitvector/invalid/bitvec_2_zero_3",
            "bitvector/invalid/bitvec_4_zero_5",
            "bitvector/invalid/bitvec_1_zero_2",
            "bitvector/invalid/bitvec_3_zero_4",
            "bitvector/invalid/bitvec_4_random_5",
            "bitvector/invalid/bitvec_5_zero_6"),
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
}
