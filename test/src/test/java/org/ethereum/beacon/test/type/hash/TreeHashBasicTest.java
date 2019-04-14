package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Container for basic types tree hash tests, no containers, lists etc. */
public class TreeHashBasicTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<TreeHashBasicTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " " + getVersion() + '\"';
  }
}
