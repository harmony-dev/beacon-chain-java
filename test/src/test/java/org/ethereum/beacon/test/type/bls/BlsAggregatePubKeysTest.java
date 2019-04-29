package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Container for bls aggregate public keys tests */
public class BlsAggregatePubKeysTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<BlsAggregatePubKeysCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " [" + String.join(",", getForks()) + "]\"";
  }
}
