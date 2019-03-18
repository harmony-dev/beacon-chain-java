package org.ethereum.beacon.test.type;

import java.util.List;

/**
 * Container for ssz tests <a
 * href="https://github.com/ethereum/eth2.0-test-generators/tree/master/ssz">https://github.com/ethereum/eth2.0-test-generators/tree/master/ssz</a>
 */
public class SszTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<SszTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
