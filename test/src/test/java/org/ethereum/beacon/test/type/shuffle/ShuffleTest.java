package org.ethereum.beacon.test.type.shuffle;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for shuffling tests <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/shuffling">https://github.com/ethereum/eth2.0-tests/tree/master/shuffling</a>
 */
public class ShuffleTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<ShuffleTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
