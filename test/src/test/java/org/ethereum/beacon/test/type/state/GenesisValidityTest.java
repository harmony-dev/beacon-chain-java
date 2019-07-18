package org.ethereum.beacon.test.type.state;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for genesis state tests <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md</a>
 */
public class GenesisValidityTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<GenesisValidityTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
