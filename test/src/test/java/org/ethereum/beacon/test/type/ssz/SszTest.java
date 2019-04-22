package org.ethereum.beacon.test.type.ssz;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for ssz tests <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/ssz">https://github.com/ethereum/eth2.0-tests/tree/master/ssz</a>
 */
public class SszTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<SszTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
