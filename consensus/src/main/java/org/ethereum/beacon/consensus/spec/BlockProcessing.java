package org.ethereum.beacon.consensus.spec;

import static java.util.stream.Collectors.toList;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;
import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Block processing part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#per-block-processing">Per-block
 *     processing</a> in the spec.
 */
public interface BlockProcessing extends HelperFunction {

  default void verify_block_header(BeaconState state, BeaconBlock block) {
    Hash32 headerRoot = signed_root(block);

    // Verify that bls_verify(
    //  pubkey=state.validator_registry[get_beacon_proposer_index(state, state.slot)].pubkey,
    //  message=proposal_root,
    //  signature=block.signature,
    //  domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_PROPOSAL)).
    ValidatorIndex proposerIndex = get_beacon_proposer_index(state, state.getSlot());
    BLSPubkey publicKey = state.getValidatorRegistry().get(proposerIndex).getPubKey();
    UInt64 domain = get_domain(state.getFork(), get_current_epoch(state), BEACON_PROPOSER);

    assertTrue(bls_verify(publicKey, headerRoot, block.getSignature(), domain));
  }

  default void process_block_header(MutableBeaconState state, BeaconBlock block) {
    // Verify that the slots match
    assertTrue(block.getSlot().equals(state.getSlot()));
    // Verify that the parent matches
    assertTrue(block.getPreviousBlockRoot().equals(signed_root(state.getLatestBlockHeader())));
    // Save current block as the new latest block
    state.setLatestBlockHeader(get_temporary_block_header(block));
  }

  default void verify_randao(BeaconState state, BeaconBlock block) {
    // Let proposer = state.validator_registry[get_beacon_proposer_index(state, state.slot)].
    ValidatorRecord proposer =
        state
            .getValidatorRegistry()
            .get(get_beacon_proposer_index(state, state.getSlot()));

    /* assert bls_verify(
        pubkey=proposer.pubkey,
        message_hash=hash_tree_root(get_current_epoch(state)),
        signature=block.body.randao_reveal,
        domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_RANDAO)
       ) */
    assertTrue(bls_verify(
        proposer.getPubKey(),
        hash_tree_root(get_current_epoch(state)),
        block.getBody().getRandaoReveal(),
        get_domain(state.getFork(), get_current_epoch(state), RANDAO)));
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
        attester_slashing.getAttestation1().getCustodyBit0Indices().intersection(
            attester_slashing.getAttestation2().getCustodyBit0Indices()).stream()
            .filter(index -> !state.getValidatorRegistry().get(index).getSlashed())
            .collect(toList());

    for (ValidatorIndex index : slashable_indices) {
      slash_validator(state, index);
    }
  }

  default void verify_attestation(BeaconState state, Attestation attestation) {
    AttestationData data = attestation.getData();

    // Verify that attestation.data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot
    //    < attestation.data.slot + SLOTS_PER_EPOCH
    assertTrue(state.getSlot()
        .greaterEqual(data.getSlot().plus(getConstants().getMinAttestationInclusionDelay())));
    assertTrue(state.getSlot().less(data.getSlot().plus(getConstants().getSlotsPerEpoch())));

    // # Can't submit attestations too quickly
    // assert attestation.data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot
    assertTrue(data.getSlot()
        .plus(getConstants().getMinAttestationInclusionDelay()).lessEqual(state.getSlot()));

    /* # Verify that the justified epoch and root is correct
    if slot_to_epoch(attestation.data.slot) >= get_current_epoch(state):
        # Case 1: current epoch attestations
        assert attestation.data.source_epoch == state.current_justified_epoch
        assert attestation.data.source_root == state.current_justified_root
    else:
        # Case 2: previous epoch attestations
        assert attestation.data.source_epoch == state.previous_justified_epoch
        assert attestation.data.source_root == state.previous_justified_root */

    if (slot_to_epoch(data.getSlot()).greaterEqual(get_current_epoch(state))) {
      assertTrue(data.getSourceEpoch().equals(state.getCurrentJustifiedEpoch()));
      assertTrue(data.getSourceRoot().equals(state.getCurrentJustifiedRoot()));
    } else {
      assertTrue(data.getSourceEpoch().equals(state.getPreviousJustifiedEpoch()));
      assertTrue(data.getSourceRoot().equals(state.getPreviousJustifiedRoot()));
    }

    // Check crosslink data
    /*  assert attestation.data.crosslink_data_root == ZERO_HASH  # [to be removed in phase 1]
        crosslinks = state.current_crosslinks if slot_to_epoch(attestation.data.slot) == get_current_epoch(state) else state.previous_crosslinks
        assert crosslinks[attestation.data.shard] == attestation.data.previous_crosslink */
    assertTrue(Hash32.ZERO.equals(data.getCrosslinkDataRoot()));
    ReadList<ShardNumber, Crosslink> crosslinks =
        slot_to_epoch(data.getSlot()).equals(get_current_epoch(state)) ?
            state.getCurrentCrosslinks() : state.getPreviousCrosslinks();
    assertTrue(hash_tree_root(crosslinks.get(data.getShard())).equals(data.getPreviousCrosslinkRoot()));

    //  assert attestation.custody_bitfield == b'\x00' * len(attestation.custody_bitfield)  # [TO BE REMOVED IN PHASE 1]
    assertTrue(attestation.getCustodyBitfield().isZero());
    //  assert attestation.aggregation_bitfield != b'\x00' * len(attestation.aggregation_bitfield)
    assertTrue(!attestation.getAggregationBitfield().isZero());

    //  crosslink_committee = [
    //      committee for committee, shard in get_crosslink_committees_at_slot(state, attestation.data.slot)
    //      if shard == attestation.data.shard
    //  ][0]
    Optional<ShardCommittee> crosslink_committee_opt =
        get_crosslink_committees_at_slot(state, data.getSlot()).stream()
        .filter(c -> c.getShard().equals(data.getShard()))
        .findFirst();
    assertTrue(crosslink_committee_opt.isPresent());
    List<ValidatorIndex> crosslink_committee = crosslink_committee_opt.get().getCommittee();

    //  for i in range(len(crosslink_committee)):
    //      if get_bitfield_bit(attestation.aggregation_bitfield, i) == 0b0:
    //          assert get_bitfield_bit(attestation.custody_bitfield, i) == 0b0
    for (int i = 0; i < crosslink_committee.size(); i++) {
      if (attestation.getAggregationBitfield().getBit(i) == false) {
        assertTrue(attestation.getCustodyBitfield().getBit(i) == false);
      }
    }

    //  participants = get_attestation_participants(state, attestation.data, attestation.aggregation_bitfield)
    List<ValidatorIndex> participants =
        get_attestation_participants(state, data, attestation.getAggregationBitfield());

    //  custody_bit_1_participants = get_attestation_participants(state, attestation.data, attestation.custody_bitfield)
    List<ValidatorIndex> custody_bit_1_participants =
        get_attestation_participants(state, data, attestation.getCustodyBitfield());
    //  custody_bit_0_participants = [i in participants for i not in custody_bit_1_participants]
    List<ValidatorIndex> custody_bit_0_participants = participants.stream()
        .filter(i -> !custody_bit_1_participants.contains(i)).collect(Collectors.toList());

    //  assert bls_verify_multiple(
    //      pubkeys=[
    //          bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_0_participants]),
    //          bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_1_participants]),
    //      ],
    //      messages=[
    //          hash_tree_root(AttestationDataAndCustodyBit(data=attestation.data, custody_bit=0b0)),
    //          hash_tree_root(AttestationDataAndCustodyBit(data=attestation.data, custody_bit=0b1)),
    //      ],
    //      signature=attestation.aggregate_signature,
    //      domain=get_domain(state.fork, slot_to_epoch(attestation.data.slot), DOMAIN_ATTESTATION),
    //  )
    List<BLSPubkey> pubKeys1 = mapIndicesToPubKeys(state, custody_bit_0_participants);
    PublicKey groupPublicKey1 = bls_aggregate_pubkeys(pubKeys1);
    List<BLSPubkey> pubKeys2 = mapIndicesToPubKeys(state, custody_bit_1_participants);
    PublicKey groupPublicKey2 = bls_aggregate_pubkeys(pubKeys2);
    assertTrue(bls_verify_multiple(
        Arrays.asList(groupPublicKey1, groupPublicKey2),
        Arrays.asList(
            hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
            hash_tree_root(new AttestationDataAndCustodyBit(data, true))),
        attestation.getSignature(),
        get_domain(state.getFork(), slot_to_epoch(data.getSlot()), ATTESTATION)));
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
        state.getSlot(),
        get_beacon_proposer_index(state, state.getSlot())
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
