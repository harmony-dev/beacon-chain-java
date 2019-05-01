//package org.ethereum.beacon.test;
//
//import org.ethereum.beacon.consensus.BeaconChainSpec;
//import org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner;
//import org.ethereum.beacon.test.runner.hash.TreeHashContainerRunner;
//import org.ethereum.beacon.test.runner.hash.TreeHashListRunner;
//import org.ethereum.beacon.test.runner.hash.TreeHashVectorRunner;
//import org.ethereum.beacon.test.type.hash.TreeHashBasicTest;
//import org.ethereum.beacon.test.type.hash.TreeHashContainerTest;
//import org.ethereum.beacon.test.type.hash.TreeHashListTest;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Optional;
//
//import static org.junit.Assert.fail;
//
//public class TreeHashTests extends TestUtils {
//  private String TESTS_DIR = "tree_hash";
//  private BeaconChainSpec spec;
//
//  public TreeHashTests() {
//    this.spec = BeaconChainSpec.createWithDefaults();
//  }
//
//  @Test
//  @Ignore("Remove when yaml is added to `eth2.0-test` repository")
//  public void testBasicTypesTreeHash() {
//    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_types.yaml");
//    TreeHashBasicTest test =
//        readTest(getResourceFile(testFilePath.toString()), TreeHashBasicTest.class);
//    Optional<String> errors =
//        runAllCasesInTest(
//            test,
//            testCase -> {
//              TreeHashBasicRunner testRunner = new TreeHashBasicRunner(testCase, spec);
//              return testRunner.run();
//            },
//            TreeHashBasicTest.class);
//    if (errors.isPresent()) {
//      System.out.println(errors.get());
//      fail();
//    }
//  }
//
//  @Test
//  @Ignore("Remove when yaml is added to `eth2.0-test` repository")
//  public void testListTypeTreeHash() {
//    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_lists.yaml");
//    TreeHashListTest test =
//        readTest(getResourceFile(testFilePath.toString()), TreeHashListTest.class);
//    Optional<String> errors =
//        runAllCasesInTest(
//            test,
//            testCase -> {
//              TreeHashListRunner testRunner = new TreeHashListRunner(testCase, spec);
//              return testRunner.run();
//            },
//            TreeHashListTest.class);
//    if (errors.isPresent()) {
//      System.out.println(errors.get());
//      fail();
//    }
//  }
//
//  @Test
//  @Ignore("Remove when yaml is added to `eth2.0-test` repository")
//  public void testVectorTypeTreeHash() {
//    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_vectors.yaml");
//    TreeHashListTest test =
//        readTest(getResourceFile(testFilePath.toString()), TreeHashListTest.class);
//    Optional<String> errors =
//        runAllCasesInTest(
//            test,
//            testCase -> {
//              TreeHashVectorRunner testRunner = new TreeHashVectorRunner(testCase, spec);
//              return testRunner.run();
//            },
//            TreeHashListTest.class);
//    if (errors.isPresent()) {
//      System.out.println(errors.get());
//      fail();
//    }
//  }
//
//  @Test
//  @Ignore("Remove when yaml is added to `eth2.0-test` repository")
//  public void testContainerTypeTreeHash() {
//    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "basic_containers.yaml");
//    TreeHashContainerTest test =
//        readTest(getResourceFile(testFilePath.toString()), TreeHashContainerTest.class);
//    Optional<String> errors =
//        runAllCasesInTest(
//            test,
//            testCase -> {
//              TreeHashContainerRunner testRunner = new TreeHashContainerRunner(testCase, spec);
//              return testRunner.run();
//            },
//            TreeHashContainerTest.class);
//    if (errors.isPresent()) {
//      System.out.println(errors.get());
//      fail();
//    }
//  }
//}
