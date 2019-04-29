package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Container for bls message hash test cases, uncompressed */
public class BlsMessageHashTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<BlsMessageHashCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " [" + String.join(",", getForks()) + "]\"";
  }
}
