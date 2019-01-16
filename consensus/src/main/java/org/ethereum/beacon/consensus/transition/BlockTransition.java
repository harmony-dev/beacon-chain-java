package org.ethereum.beacon.consensus.transition;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.state.ValidatorRegistryUpdater;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.DepositRootVote;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class BlockTransition implements StateTransition<BeaconStateEx> {
  private final ChainSpec spec;

  public BlockTransition(ChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconBlock block, BeaconStateEx stateEx) {
    SpecHelpers specHelpers = new SpecHelpers(spec);

    MutableBeaconState state = stateEx.getCanonicalState().createMutableCopy();

    /*
      RANDAO
      Let proposer = state.validator_registry[get_beacon_proposer_index(state, state.slot)].
      Set state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] =
        hash(xor(state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH], block.randao_reveal))
      Set proposer.randao_commitment = block.randao_reveal.
      Set proposer.randao_layers = 0.
    */

    int proposerIdx = specHelpers.get_beacon_proposer_index(state, state.getSlot()).getValue();
    state.withValidatorRecord(
        proposerIdx,
        vb -> vb.withRandaoCommitment(block.getRandaoReveal()).withRandaoLayers(UInt64.ZERO));

    /*
     Deposit root
     If block.deposit_root is deposit_root_vote.deposit_root for some deposit_root_vote
       in state.deposit_root_votes, set deposit_root_vote.vote_count += 1.
     Otherwise, append to state.deposit_root_votes a
       new DepositRootVote(deposit_root=block.deposit_root, vote_count=1).
    */

    int depositIdx = -1;
    for (int i = 0; i < state.getDepositRootVotes().size(); i++) {
      if (block.getDepositRoot().equals(state.getDepositRootVotes().get(i).getDepositRoot())) {
        depositIdx = i;
        break;
      }
    }
    if (depositIdx >= 0) {
      state.withDepositRootVote(
          depositIdx,
          vote -> new DepositRootVote(vote.getDepositRoot(), vote.getVoteCount().increment()));
    } else {
      state.withNewDepositRootVote(new DepositRootVote(block.getDepositRoot(), UInt64.valueOf(1)));
    }

    /*
       For each proposer_slashing in block.body.proposer_slashings:
       Run penalize_validator(state, proposer_slashing.proposer_index).
    */
    for (ProposerSlashing proposer_slashing : block.getBody().getProposerSlashings()) {
      specHelpers.penalize_validator(state, proposer_slashing.getProposerIndex().getValue());
    }

    /*
       For each casper_slashing in block.body.casper_slashings:
       Let slashable_vote_data_1 = casper_slashing.slashable_vote_data_1.
       Let slashable_vote_data_2 = casper_slashing.slashable_vote_data_2.
       Let indices(slashable_vote_data) =
          slashable_vote_data.custody_bit_0_indices + slashable_vote_data.custody_bit_1_indices.
       Let intersection = [x for x in indices(slashable_vote_data_1) if x in indices(slashable_vote_data_2)].
       For each validator index i in intersection run penalize_validator(state, i)
           if state.validator_registry[i].penalized_slot > state.slot.
    */
    for (CasperSlashing casper_slashing : block.getBody().getCasperSlashings()) {
      Set<UInt24> indices_1 = new HashSet<>();
      indices_1.addAll(asList(casper_slashing.getSlashableVoteData1().getCustodyBit0Indices()));
      indices_1.addAll(asList(casper_slashing.getSlashableVoteData1().getCustodyBit1Indices()));
      Set<UInt24> indices_2 = new HashSet<>();
      indices_2.addAll(asList(casper_slashing.getSlashableVoteData2().getCustodyBit0Indices()));
      indices_2.addAll(asList(casper_slashing.getSlashableVoteData2().getCustodyBit1Indices()));
      Set<UInt24> intersection = indices_1;
      intersection.retainAll(indices_2);
      for (UInt24 index : intersection) {
        if (state
                .getValidatorRegistry()
                .get(index.getValue())
                .getPenalizedSlot()
                .compareTo(state.getSlot())
            > 0) {
          specHelpers.penalize_validator(state, index.getValue());
        }
      }
    }

    /*
       Attestations

       For each attestation in block.body.attestations:
       Append PendingAttestationRecord(
           data=attestation.data,
           participation_bitfield=attestation.participation_bitfield,
           custody_bitfield=attestation.custody_bitfield,
           slot_included=state.slot
       ) to state.latest_attestations.
    */
    for (Attestation attestation : block.getBody().getAttestations()) {
      PendingAttestationRecord record =
          new PendingAttestationRecord(
              attestation.getData(),
              attestation.getParticipationBitfield(),
              attestation.getCustodyBitfield(),
              state.getSlot());
      state.withNewLatestAttestation(record);
    }

    /*
       Deposits

       For each deposit in block.body.deposits:
       Run the following:

       process_deposit(
           state=state,
           pubkey=deposit.deposit_data.deposit_input.pubkey,
           amount=deposit.deposit_data.amount,
           proof_of_possession=deposit.deposit_data.deposit_input.proof_of_possession,
           withdrawal_credentials=deposit.deposit_data.deposit_input.withdrawal_credentials,
           randao_commitment=deposit.deposit_data.deposit_input.randao_commitment,
           custody_commitment=deposit.deposit_data.deposit_input.custody_commitment,
       )
    */
    final ValidatorRegistryUpdater registryUpdater =
        ValidatorRegistryUpdater.fromState(state, spec);
    for (Deposit deposit : block.getBody().getDeposits()) {
      registryUpdater.processDeposit(deposit);
    }

    /*
     Exits

     For each exit in block.body.exits:
       Run initiate_validator_exit(state, exit.validator_index).
    */
    for (Exit exit : block.getBody().getExits()) {
      specHelpers.initiate_validator_exit(state, exit.getValidatorIndex().getValue());
    }

    return new BeaconStateEx(state.validate(), block.getHash());
  }
}
