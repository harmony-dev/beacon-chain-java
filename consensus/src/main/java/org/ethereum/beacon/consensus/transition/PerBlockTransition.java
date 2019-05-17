package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;

/**
 * Per-block transition, which happens at every block.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#per-block-processing">Per-block
 *     processing</a> in the spec.
 */
public class PerBlockTransition implements BlockTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerBlockTransition.class);

  private final BeaconChainSpec spec;

  public PerBlockTransition(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx, BeaconBlock block) {
    logger.trace(() -> "Applying block transition to state: (" +
        spec.hash_tree_root(stateEx).toStringShort() + ") "
        + stateEx.toString(spec.getConstants(), spec::signing_root) + ", Block: "
        + block.toString(spec.getConstants(), stateEx.getGenesisTime(), spec::signing_root));

    TransitionType.BLOCK.checkCanBeAppliedAfter(stateEx.getTransition());

    MutableBeaconState state = stateEx.createMutableCopy();

    // For every block except the genesis block,
    // run process_block_header(state, block), process_randao(state, block) and process_eth1_data(state, block).
    spec.process_block_header(state, block);
    spec.process_randao(state, block);
    spec.process_eth1_data(state, block);

    // For each proposer_slashing in block.body.proposer_slashings, run the following function:
    block.getBody().getProposerSlashings().forEach(
        proposer_slashing -> spec.process_proposer_slashing(state, proposer_slashing));

    // For each attester_slashing in block.body.attester_slashings, run the following function:
    block.getBody().getAttesterSlashings().forEach(
        attester_slashing -> spec.process_attester_slashing(state, attester_slashing));

    // For each attestation in block.body.attestations, run the following function:
    block.getBody().getAttestations().forEach(
        attestation -> spec.process_attestation(state, attestation));

    // For each deposit in block.body.deposits, run process_deposit(state, deposit).
    block.getBody().getDeposits().forEach(
        deposit -> spec.process_deposit(state, deposit));

    // For each exit in block.body.voluntary_exits, run the following function:
    block.getBody().getVoluntaryExits().forEach(
        voluntary_exit -> spec.process_voluntary_exit(state, voluntary_exit));

    // For each transfer in block.body.transfers, run the following function:
    block.getBody().getTransfers().forEach(
        transfer -> spec.process_transfer(state, transfer));

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(), TransitionType.BLOCK);

    logger.trace(() -> "Block transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " +
        ret.toString(spec.getConstants(), spec::signing_root));

    return ret;
  }
}
