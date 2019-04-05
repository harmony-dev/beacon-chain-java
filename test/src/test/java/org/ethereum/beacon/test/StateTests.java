package org.ethereum.beacon.test;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.ethereum.beacon.test.runner.StateRunner;
import org.ethereum.beacon.test.type.StateTest;
import org.junit.Test;

public class StateTests extends TestUtils {
  private String TESTS_DIR = "state";

  public StateTests() {}

  @Test
  // TODO remove exclusions after hash_tree_root and state_root tests pass
  public void testState() {
    Path stateTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDirWithExclusion(
        stateTestsPath,
        StateTest.class,
        testCase -> new StateRunner(testCase).run(),
        "test_skipped_slots",
        "test_empty_epoch_transition",
        "test_historical_batch");
  }
}
