package org.ethereum.beacon.test.type.state;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for state tests <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/state">https://github.com/ethereum/eth2.0-tests/tree/master/state</a>
 */
public class StateTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<StateTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
