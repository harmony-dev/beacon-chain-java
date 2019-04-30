package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.ssz.SszGenericRunner;
import org.ethereum.beacon.test.type.ssz.SszGenericTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/** SSZ tests, generic with primitive values and static, with known container types */
public class SszTests extends TestUtils {

  @Test
  public void testSszGeneric() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, "ssz_generic", "uint");
    runTestsInResourceDir(
        testFileDir,
        SszGenericTest.class,
        input -> {
          SszGenericRunner testRunner = new SszGenericRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }
}
