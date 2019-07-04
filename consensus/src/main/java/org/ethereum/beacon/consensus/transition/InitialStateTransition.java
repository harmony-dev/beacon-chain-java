package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * Produces initial beacon state.
 *
 * <p>Requires input {@code block} to be a Genesis block, {@code state} parameter is ignored.
 * Preferred input for {@code state} parameter is {@link BeaconState#getEmpty()}.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#genesis-state">Genesis
 *     state</a> in the spec.
 */
public class InitialStateTransition implements BlockTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(InitialStateTransition.class);

  private final ChainStart depositContractStart;
  private final BeaconChainSpec spec;

  public InitialStateTransition(ChainStart depositContractStart,
      BeaconChainSpec spec) {
    this.depositContractStart = depositContractStart;
    this.spec = spec;
  }

  public BeaconStateEx apply(BeaconBlock block) {
    return apply(null, block);
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx state, BeaconBlock block) {
    assert block.getSlot().equals(spec.getConstants().getGenesisSlot());

    BeaconState genesisState =
        spec.initialize_beacon_state_from_eth1(
            depositContractStart.getEth1Data().getBlockHash(),
            depositContractStart.getTime(),
            depositContractStart.getInitialDeposits()
        );

    BeaconStateExImpl ret = new BeaconStateExImpl(genesisState, TransitionType.INITIAL);

    logger.debug(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants(), spec::signing_root));

    return ret;
  }
}
