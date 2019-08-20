package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.GenesisInitCase;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.List;
import java.util.Optional;

/**
 * TestRunner for {@link GenesisInitCase}
 *
 * <p>Test format description: <a
 * href=https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/initialization.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/genesis/initialization.md</a>
 */
public class GenesisInitRunner implements Runner {
  private GenesisInitCase testCase;
  private BeaconChainSpec spec;
  private String handler;

  public GenesisInitRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof GenesisInitCase)) {
      throw new RuntimeException("TestCase runner accepts only GenesisInitCase.class as input!");
    }
    this.testCase = (GenesisInitCase) testCase;
    this.spec = spec;
    this.handler = handler;
  }

  public Optional<String> run() {
    BeaconState latestState =
        processInitialization(
            Hash32.fromHexString(testCase.getEth1BlockHash()),
            testCase.getEth1Timestamp(),
            testCase.getDeposits());
    return StateComparator.compare(testCase.getState(spec.getConstants()), latestState, spec);
  }

  private BeaconState processInitialization(
      Hash32 eth1BlockHash, Long eth1Timestamp, List<Deposit> initialDeposits) {
    return spec.initialize_beacon_state_from_eth1(
        eth1BlockHash, Time.of(eth1Timestamp), initialDeposits);
  }
}
