//package org.ethereum.beacon.test;
//
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import org.ethereum.beacon.test.runner.state.StateRunner;
//import org.ethereum.beacon.test.type.state.StateTest;
//import org.junit.Ignore;
//import org.junit.Test;
//
//public class StateTests extends TestUtils {
//  private String TESTS_DIR = "state";
//
//  public StateTests() {}
//
//  @Test
//  @Ignore("signed_root and hash_tree_root results do not match")
//  public void testState() {
//    Path stateTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
//    runTestsInResourceDir(
//        stateTestsPath,
//        StateTest.class,
//        testCase -> new StateRunner(testCase).run());
//  }
//}
