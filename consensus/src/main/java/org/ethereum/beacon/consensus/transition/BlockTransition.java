package org.ethereum.beacon.consensus.transition;

import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt64;

public class BlockTransition implements
    org.ethereum.beacon.consensus.BlockTransition<BeaconStateEx> {
  private final ChainSpec spec;
  private final SpecHelpers specHelpers;

  public BlockTransition(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.spec = specHelpers.getChainSpec();
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx, BeaconBlock block) {
    MutableBeaconState state = stateEx.getCanonicalState().createMutableCopy();

    /*
      RANDAO
      Let proposer = state.validator_registry[get_beacon_proposer_index(state, state.slot)].
      Set state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] =
      hash(state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] + block.randao_reveal).
    */
    state.getLatestRandaoMixes().update(state.getSlot().modulo(spec.getLatestRandaoMixesLength()),
        rm -> specHelpers.hash(rm.concat(block.getRandaoReveal())));

    /*
     Eth1 data
     If block.eth1_data equals eth1_data_vote.eth1_data for some eth1_data_vote
       in state.eth1_data_votes, set eth1_data_vote.vote_count += 1.
     Otherwise, append to state.eth1_data_votes
       a new Eth1DataVote(eth1_data=block.eth1_data, vote_count=1).
    */

    int depositIdx = -1;
    for (int i = 0; i < state.getEth1DataVotes().size(); i++) {
      if (block.getEth1Data().equals(state.getEth1DataVotes().get(i).getEth1Data())) {
        depositIdx = i;
        break;
      }
    }
    if (depositIdx >= 0) {
      state.getEth1DataVotes().update(depositIdx,
          vote -> new Eth1DataVote(vote.getEth1Data(), vote.getVoteCount().increment()));
    } else {
      state.getEth1DataVotes().add(new Eth1DataVote(block.getEth1Data(), UInt64.valueOf(1)));
    }

    /*
       For each proposer_slashing in block.body.proposer_slashings:
       Run penalize_validator(state, proposer_slashing.proposer_index).
    */
    for (ProposerSlashing proposer_slashing : block.getBody().getProposerSlashings()) {
      specHelpers.penalize_validator(state, proposer_slashing.getProposerIndex());
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
      List<ValidatorIndex> intersection = specHelpers.custodyIndexIntersection(casper_slashing);
      for (ValidatorIndex index : intersection) {
        if (state.getValidatorRegistry().get(index).getPenalizedSlot().greater(
                state.getSlot())) {
          specHelpers.penalize_validator(state, index);
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
      state.getLatestAttestations().add(record);
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
       )
    */
    for (Deposit deposit : block.getBody().getDeposits()) {
      DepositData depositData = deposit.getDepositData();
      DepositInput depositInput = depositData.getDepositInput();
      specHelpers.process_deposit(state,
          depositInput.getPubKey(),
          depositData.getValue(),
          depositInput.getProofOfPossession(),
          depositInput.getWithdrawalCredentials()
      );
    }

    /*
     Exits

     For each exit in block.body.exits:
       Run initiate_validator_exit(state, exit.validator_index).
    */
    for (Exit exit : block.getBody().getExits()) {
      specHelpers.initiate_validator_exit(state, exit.getValidatorIndex());
    }

    return new BeaconStateEx(state.createImmutable(), specHelpers.hash_tree_root(block));
  }
}
