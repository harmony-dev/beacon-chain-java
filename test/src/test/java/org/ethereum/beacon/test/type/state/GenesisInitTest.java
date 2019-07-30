package org.ethereum.beacon.test.type.state;

import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.util.List;

/**
 * Container for genesis state tests <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/initialization.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/initialization.md</a>
 */
public class GenesisInitTest extends TestSkeleton {
  public List<TestCase> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<GenesisInitTestCase> testCases) {
    this.testCases = (List<TestCase>) (List<?>) testCases;
  }
}
