package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.test.runner.hash.TreeHashCompositeRunner;
import org.ethereum.beacon.test.runner.hash.TreeHashContainerRunner;
import org.ethereum.beacon.test.runner.hash.TreeHashListRunner;
import org.ethereum.beacon.test.runner.hash.TreeHashSimpleRunner;
import org.ethereum.beacon.test.runner.hash.TreeHashVectorRunner;
import org.ethereum.beacon.test.type.hash.TreeHashCompositeTest;
import org.ethereum.beacon.test.type.hash.TreeHashContainerTest;
import org.ethereum.beacon.test.type.hash.TreeHashListTest;
import org.ethereum.beacon.test.type.hash.TreeHashSimpleTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.fail;

public class TreeHashTests extends TestUtils {
  private String TESTS_DIR = "tree_hash";
  private BeaconChainSpec spec;

  public TreeHashTests() {
    this.spec = BeaconChainSpec.createWithDefaults();
  }

  @Test
  public void testSimpleTypeTreeHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_types.yaml");
    TreeHashSimpleTest test = readTest(getResourceFile(testFilePath.toString()), TreeHashSimpleTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            testCase -> {
              TreeHashSimpleRunner testRunner = new TreeHashSimpleRunner(testCase, spec);
              return testRunner.run();
            },
            TreeHashSimpleTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testListTypeTreeHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_lists.yaml");
    TreeHashListTest test = readTest(getResourceFile(testFilePath.toString()), TreeHashListTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            testCase -> {
              TreeHashListRunner testRunner = new TreeHashListRunner(testCase, spec);
              return testRunner.run();
            },
            TreeHashListTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testVectorTypeTreeHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_vectors.yaml");
    TreeHashListTest test = readTest(getResourceFile(testFilePath.toString()), TreeHashListTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            testCase -> {
              TreeHashVectorRunner testRunner = new TreeHashVectorRunner(testCase, spec);
              return testRunner.run();
            },
            TreeHashListTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testContainerTypeTreeHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_containers.yaml");
    TreeHashContainerTest test = readTest(getResourceFile(testFilePath.toString()), TreeHashContainerTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            testCase -> {
              TreeHashContainerRunner testRunner = new TreeHashContainerRunner(testCase, spec);
              return testRunner.run();
            },
            TreeHashContainerTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testNestedCompositeTypeTreeHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "nested_composites.yaml");
    TreeHashCompositeTest test = readTest(getResourceFile(testFilePath.toString()), TreeHashCompositeTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            testCase -> {
              TreeHashCompositeRunner testRunner = new TreeHashCompositeRunner(testCase, spec);
              return testRunner.run();
            },
            TreeHashCompositeTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }
}
