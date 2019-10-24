package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.GenesisValidityCase;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for {@link GenesisValidityCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/validity.md</a>
 */
public class GenesisValidityRunner implements Runner {
  private GenesisValidityCase testCase;
  private BeaconChainSpec spec;
  private String handler;

  public GenesisValidityRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof GenesisValidityCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only GenesisValidityCase.class as input!");
    }
    this.testCase = (GenesisValidityCase) testCase;
    this.spec = spec;
    this.handler = handler;
  }

  public Optional<String> run() {
    boolean validity = checkValidity(testCase.getGenesis(spec.getConstants()));
    return assertEquals(testCase.isValid(), validity);
  }

  private boolean checkValidity(BeaconState genesisState) {
    return spec.is_valid_genesis_state(genesisState);
  }
}
