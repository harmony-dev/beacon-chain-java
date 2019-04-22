package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/** Tree hash test with list/vector cases */
public class TreeHashListTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<TreeHashListTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + getTitle() + " " + getVersion() + '\"';
  }
}
