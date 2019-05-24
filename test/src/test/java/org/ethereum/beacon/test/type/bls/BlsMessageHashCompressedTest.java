package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Container for bls message hash test cases, compressed */
public class BlsMessageHashCompressedTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<BlsMessageHashCompressedCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " [" + String.join(",", getForks()) + "]\"";
  }
}
