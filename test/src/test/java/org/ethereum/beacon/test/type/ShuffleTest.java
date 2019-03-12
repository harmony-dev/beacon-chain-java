package org.ethereum.beacon.test.type;

import java.util.List;

/**
 * Container for shuffling tests <a
 * href="https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling">https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling</a>
 */
public class ShuffleTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<ShuffleTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
