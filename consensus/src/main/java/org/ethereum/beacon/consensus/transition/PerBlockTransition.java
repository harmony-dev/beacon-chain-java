package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Per-block transition function.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/core/0_beacon-chain.md#per-block-processing">Per-block
 *     processing</a> in the spec.
 */
public class PerBlockTransition implements BlockTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerBlockTransition.class);

  private final ChainSpec spec;
  private final SpecHelpers specHelpers;

  public PerBlockTransition(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.spec = specHelpers.getChainSpec();
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx, BeaconBlock block) {
    logger.trace(() -> "Applying block transition to state: (" +
        specHelpers.hash_tree_root(stateEx).toStringShort() + ") "
        + stateEx.toString(spec) + ", Block: "
        + block.toString(spec, stateEx.getGenesisTime(), specHelpers::hash_tree_root));

    TransitionType.BLOCK.checkCanBeAppliedAfter(stateEx.getTransition());

    MutableBeaconState state = stateEx.createMutableCopy();

    /*
    RANDAO
    Set state.latest_randao_mixes[get_current_epoch(state) % LATEST_RANDAO_MIXES_LENGTH] =
        xor(get_randao_mix(state, get_current_epoch(state)), hash(block.randao_reveal)).
    */
    state.getLatestRandaoMixes().update(
            specHelpers.get_current_epoch(state).modulo(spec.getLatestRandaoMixesLength()),
            rm -> Hash32.wrap(Bytes32s.xor(
                  specHelpers.get_randao_mix(state, specHelpers.get_current_epoch(state)),
                  specHelpers.hash(block.getRandaoReveal()))));

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
       Run slash_validator(state, proposer_slashing.proposer_index).
    */
    for (ProposerSlashing proposer_slashing : block.getBody().getProposerSlashings()) {
      specHelpers.slash_validator(state, proposer_slashing.getProposerIndex());
    }

    /*
       For each attester_slashing in block.body.attester_slashings:
         Let slashable_attestation_1 = attester_slashing.slashable_attestation_1.
         Let slashable_attestation_2 = attester_slashing.slashable_attestation_2.
         Let slashable_indices = [index for index in slashable_attestation_1.validator_indices
             if index in slashable_attestation_2.validator_indices
                 and state.validator_registry[index].initiated_exit > get_current_epoch(state)].
         Run slash_validator(state, index) for each index in slashable_indices.
    */
    for (AttesterSlashing attester_slashing : block.getBody().getAttesterSlashings()) {
      ReadList<Integer, ValidatorIndex> intersection =
          attester_slashing.getSlashableAttestation1().getValidatorIndices().intersection(
          attester_slashing.getSlashableAttestation2().getValidatorIndices());
      for (ValidatorIndex index : intersection) {
        if (!state.getValidatorRegistry().get(index).getSlashed()) {
          specHelpers.slash_validator(state, index);
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
              attestation.getAggregationBitfield(),
              attestation.getData(),
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
          depositData.getAmount(),
          depositInput.getProofOfPossession(),
          depositInput.getWithdrawalCredentials()
      );
    }

    /*
     Exits

     For each exit in block.body.exits:
       Run initiate_validator_exit(state, exit.validator_index).
    */
    for (VoluntaryExit voluntaryExit : block.getBody().getExits()) {
      specHelpers.initiate_validator_exit(state, voluntaryExit.getValidatorIndex());
    }

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(),
        specHelpers.hash_tree_root(block), TransitionType.BLOCK);

    logger.trace(() -> "Block transition result state: (" +
        specHelpers.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec));

    return ret;
  }
}
