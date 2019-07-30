package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.GenesisValidityTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for {@link StateTestCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md</a>
 */
public class GenesisValidityRunner implements Runner {
  private GenesisValidityTestCase testCase;
  private BeaconChainSpec spec;
  private String handler;

  public GenesisValidityRunner(TestCase testCase, BeaconChainSpec spec, String handler) {
    if (!(testCase instanceof GenesisValidityTestCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only GenesisValidityTestCase.class as input!");
    }
    this.testCase = (GenesisValidityTestCase) testCase;
    this.spec = spec;
    this.handler = handler;
  }

  public Optional<String> run() {
    if (!handler.equals("validity")) {
      throw new RuntimeException("This type of state test is not supported");
    }
    boolean validity = checkValidity(testCase.getGenesisState(spec.getConstants()));
    return assertEquals(testCase.isValid(), validity);
  }

  private boolean checkValidity(BeaconState genesisState) {
    return spec.is_valid_genesis_state(genesisState);
  }
}
