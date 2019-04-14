package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Container for composite tree hash tests */
public class TreeHashCompositeTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<TreeHashCompositeTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " " + getVersion() + '\"';
  }
}
