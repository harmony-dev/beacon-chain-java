package org.ethereum.beacon.test.type.ssz;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Ssz static tests with known containers */
public class SszStaticTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<SszStaticCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " [" + String.join(",", getForks()) + "]\"";
  }
}