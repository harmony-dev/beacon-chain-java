package org.ethereum.beacon.test.type.ssz;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for generic ssz tests
 */
public class SszGenericTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<SszGenericCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
