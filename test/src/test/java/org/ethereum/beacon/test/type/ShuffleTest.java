package org.ethereum.beacon.test.type;

import java.util.List;

/**
 * Container for shuffling tests <a
 * href="https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling">https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling</a>
 */
public class ShuffleTest extends TestSkeleton {
  public List<TestCase> getTest_cases() {
    return test_cases;
  }

  public void setTest_cases(List<ShuffleTestCase> test_cases) {
    this.test_cases = (List<TestCase>) (List<?>) test_cases;
  }
}
