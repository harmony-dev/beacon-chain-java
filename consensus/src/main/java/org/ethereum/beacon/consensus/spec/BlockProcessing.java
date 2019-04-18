package org.ethereum.beacon.consensus.spec;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Block processing part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#per-block-processing">Per-block
 *     processing</a> in the spec.
 */
public interface BlockProcessing extends HelperFunction {

  default void process_block_header(MutableBeaconState state, BeaconBlock block) {
    // Verify that the slots match
    assertTrue(block.getSlot().equals(state.getSlot()));
    // Verify that the parent matches
    // FIXME: signed_root should match
    assertTrue(block.getPreviousBlockRoot().equals(signed_root(state.getLatestBlockHeader())));
    // Save current block as the new latest block
    state.setLatestBlockHeader(get_temporary_block_header(block));
  }

  default void process_randao(MutableBeaconState state, BeaconBlock block) {
    // Mix it in
    state.getLatestRandaoMixes().set(get_current_epoch(state).modulo(getConstants().getLatestRandaoMixesLength()),
        Hash32.wrap(Bytes32s.xor(
            get_randao_mix(state, get_current_epoch(state)),
            hash(block.getBody().getRandaoReveal()))));
  }

  default void process_eth1_data(MutableBeaconState state, BeaconBlock block) {
    /* for eth1_data_vote in state.eth1_data_votes:
        # If someone else has already voted for the same hash, add to its counter
        if eth1_data_vote.eth1_data == block.body.eth1_data:
            eth1_data_vote.vote_count += 1
            return */
    for (int i = 0; i < state.getEth1DataVotes().size(); i++) {
      Eth1DataVote eth1_data_vote = state.getEth1DataVotes().get(i);
      // If someone else has already voted for the same hash, add to its counter
      if (eth1_data_vote.getEth1Data().equals(block.getBody().getEth1Data())) {
        state.getEth1DataVotes().update(i, vote ->
            new Eth1DataVote(vote.getEth1Data(), vote.getVoteCount().increment()));
        return;
      }
    }

    // If we're seeing this hash for the first time, make a new counter
    state.getEth1DataVotes().add(
        new Eth1DataVote(block.getBody().getEth1Data(), UInt64.valueOf(1)));
  }

  /*
    """
    Process ``ProposerSlashing`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_proposer_slashing(MutableBeaconState state, ProposerSlashing proposer_slashing) {
    slash_validator(state, proposer_slashing.getProposerIndex());
  }

  /*
    """
    Process ``AttesterSlashing`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_attester_slashing(MutableBeaconState state, AttesterSlashing attester_slashing) {
    List<ValidatorIndex> slashable_indices =
        attester_slashing.getSlashableAttestation1().getValidatorIndices().intersection(
            attester_slashing.getSlashableAttestation2().getValidatorIndices()).stream()
            .filter(index -> !state.getValidatorRegistry().get(index).getSlashed())
            .collect(toList());

    for (ValidatorIndex index : slashable_indices) {
      slash_validator(state, index);
    }
  }

  /*
   """
   Process ``Attestation`` transaction.
   Note that this function mutates ``state``.
   """
  */
  default void process_attestation(MutableBeaconState state, Attestation attestation) {
    // Apply the attestation
    PendingAttestation pending_attestation = new PendingAttestation(
        attestation.getAggregationBitfield(),
        attestation.getData(),
        attestation.getCustodyBitfield(),
        state.getSlot()
    );

    if (slot_to_epoch(attestation.getData().getSlot()).equals(get_current_epoch(state))) {
      state.getCurrentEpochAttestations().add(pending_attestation);
    } else if (slot_to_epoch(attestation.getData().getSlot()).equals(get_previous_epoch(state))) {
      state.getPreviousEpochAttestations().add(pending_attestation);
    }
  }

  /*
    """
    Process ``VoluntaryExit`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_voluntary_exit(MutableBeaconState state, VoluntaryExit exit) {
    initiate_validator_exit(state, exit.getValidatorIndex());
  }

  /*
    """
    Process ``Transfer`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_transfer(MutableBeaconState state, Transfer transfer) {
    // Process the transfer
    state.getValidatorBalances().update(transfer.getSender(),
        balance -> balance.minusSat(transfer.getAmount().plus(transfer.getFee())));
    state.getValidatorBalances().update(transfer.getRecipient(),
        balance -> balance.plusSat(transfer.getAmount()));
    state.getValidatorBalances().update(get_beacon_proposer_index(state, state.getSlot()),
        balance -> balance.plusSat(transfer.getFee()));
  }
}
