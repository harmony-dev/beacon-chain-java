package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.pow.DepositContract;

/**
 * Produces initial beacon state.
 *
 * <p>Requires input {@code block} to be a Genesis block, {@code state} parameter is ignored.
 * Preferred input for {@code state} parameter is {@link BeaconState#getEmpty()}.
 *
 * <p>Uses {@link DepositContract} to fetch registration data from the PoW chain.
 *
 * @see DepositContract
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#on-genesis">On
 *     genesis</a> in the spec.
 */
public class InitialStateTransition implements BlockTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(InitialStateTransition.class);

  private final DepositContract.ChainStart depositContractStart;
  private final SpecHelpers spec;

  public InitialStateTransition(DepositContract.ChainStart depositContractStart,
      SpecHelpers spec) {
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
        spec.get_genesis_beacon_state(
            depositContractStart.getInitialDeposits(),
            depositContractStart.getTime(),
            depositContractStart.getEth1Data());
    BeaconBlock genesisBlock = block.withStateRoot(spec.hash_tree_root(genesisState));

    BeaconStateExImpl ret = new BeaconStateExImpl(
        genesisState, spec.hash_tree_root(genesisBlock), TransitionType.INITIAL);

    logger.debug(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
