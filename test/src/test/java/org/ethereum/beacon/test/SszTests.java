package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.ssz.SszGenericRunner;
import org.ethereum.beacon.test.runner.ssz.SszStaticRunner;
import org.ethereum.beacon.test.type.ssz.SszGenericTest;
import org.ethereum.beacon.test.type.ssz.SszStaticTest;
import org.junit.Ignore;
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

  @Test
  @Ignore("Requires fixtures update")
  public void testSszStatic() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, "ssz_static", "core");
    runTestsInResourceDir(
        testFileDir,
        SszStaticTest.class,
        input -> {
          SszStaticRunner testRunner = new SszStaticRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        },
        true // run it in parallel, a lot of tests
        );
  }
}
