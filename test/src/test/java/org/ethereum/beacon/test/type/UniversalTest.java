package org.ethereum.beacon.test.type;

import java.util.List;

public class UniversalTest<V extends TestCase> extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<V> testCases) {
    this.testCases = (List<TestCase>) testCases;
  }
}
