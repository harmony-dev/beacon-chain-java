package org.ethereum.beacon.consensus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class DelegatingBeaconChainSpec implements BeaconChainSpec {

  private final BeaconChainSpec delegate;

  public DelegatingBeaconChainSpec(BeaconChainSpec delegate) {
    this.delegate = delegate;
  }

  public BeaconChainSpec getDelegate() {
    return delegate;
  }

  @Override
  public SpecConstants getConstants() {
    return delegate.getConstants();
  }

  @Override
  public ObjectHasher<Hash32> getObjectHasher() {
    return delegate.getObjectHasher();
  }

  @Override
  public Function<BytesValue, Hash32> getHashFunction() {
    return delegate.getHashFunction();
  }

  @Override
  public Hash32 hash(BytesValue data) {
    return delegate.hash(data);
  }

  @Override
  public Bytes32 xor(Bytes32 bytes1, Bytes32 bytes2) {
    return delegate.xor(bytes1, bytes2);
  }

  @Override
  public BeaconBlockHeader get_temporary_block_header(BeaconBlock block) {
    return delegate.get_temporary_block_header(block);
  }

  @Override
  public int get_epoch_committee_count(int active_validator_count) {
    return delegate.get_epoch_committee_count(active_validator_count);
  }

  @Override
  public int get_previous_epoch_committee_count(BeaconState state) {
    return delegate.get_previous_epoch_committee_count(state);
  }

  @Override
  public int get_current_epoch_committee_count(BeaconState state) {
    return delegate.get_current_epoch_committee_count(state);
  }

  @Override
  public int get_next_epoch_committee_count(BeaconState state) {
    return delegate.get_next_epoch_committee_count(state);
  }

  @Override
  public List<ShardCommittee> get_crosslink_committees_at_slot(BeaconState state, SlotNumber slot) {
    return delegate.get_crosslink_committees_at_slot(state, slot);
  }

  @Override
  public List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot, boolean registry_change) {
    return delegate.get_crosslink_committees_at_slot(state, slot, registry_change);
  }

  @Override
  public ValidatorIndex get_beacon_proposer_index(
      BeaconState state, SlotNumber slot, boolean registryChange) {
    return delegate.get_beacon_proposer_index(state, slot, registryChange);
  }

  @Override
  public ValidatorIndex get_beacon_proposer_index(BeaconState state, SlotNumber slot) {
    return delegate.get_beacon_proposer_index(state, slot);
  }

  @Override
  public boolean is_active_validator(ValidatorRecord validator, EpochNumber epoch) {
    return delegate.is_active_validator(validator, epoch);
  }

  @Override
  public List<ValidatorIndex> get_active_validator_indices(
      ReadList<ValidatorIndex, ValidatorRecord> validators, EpochNumber epochNumber) {
    return delegate.get_active_validator_indices(validators, epochNumber);
  }

  @Override
  public Hash32 get_randao_mix(BeaconState state, EpochNumber epoch) {
    return delegate.get_randao_mix(state, epoch);
  }

  @Override
  public UInt64 get_permuted_index(UInt64 index, UInt64 listSize, Bytes32 seed) {
    return delegate.get_permuted_index(index, listSize, seed);
  }

  @Override
  public List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    return delegate.get_permuted_list(indices, seed);
  }

  @Override
  public UInt64 bytes_to_int(Bytes8 bytes) {
    return delegate.bytes_to_int(bytes);
  }

  @Override
  public UInt64 bytes_to_int(BytesValue bytes) {
    return delegate.bytes_to_int(bytes);
  }

  @Override
  public BytesValue int_to_bytes1(int value) {
    return delegate.int_to_bytes1(value);
  }

  @Override
  public Bytes4 int_to_bytes4(long value) {
    return delegate.int_to_bytes4(value);
  }

  @Override
  public Bytes4 int_to_bytes4(UInt64 value) {
    return delegate.int_to_bytes4(value);
  }

  @Override
  public BytesValue int_to_bytes32(UInt64 value) {
    return delegate.int_to_bytes32(value);
  }

  @Override
  public <T> List<List<T>> split(List<T> values, int split_count) {
    return delegate.split(values, split_count);
  }

  @Override
  public List<List<ValidatorIndex>> get_shuffling(
      Hash32 seed, ReadList<ValidatorIndex, ValidatorRecord> validators, EpochNumber epoch) {
    return delegate.get_shuffling(seed, validators, epoch);
  }

  @Override
  public List<List<ValidatorIndex>> get_shuffling2(
      Hash32 seed, ReadList<ValidatorIndex, ValidatorRecord> validators, EpochNumber epoch) {
    return delegate.get_shuffling2(seed, validators, epoch);
  }

  @Override
  public boolean verify_merkle_branch(
      Hash32 leaf, List<Hash32> proof, UInt64 depth, UInt64 index, Hash32 root) {
    return delegate.verify_merkle_branch(leaf, proof, depth, index, root);
  }

  @Override
  public Gwei get_effective_balance(BeaconState state, ValidatorIndex validatorIdx) {
    return delegate.get_effective_balance(state, validatorIdx);
  }

  @Override
  public Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> validators) {
    return delegate.get_total_balance(state, validators);
  }

  @Override
  public UInt64 integer_squareroot(UInt64 n) {
    return delegate.integer_squareroot(n);
  }

  @Override
  public boolean is_power_of_two(UInt64 value) {
    return delegate.is_power_of_two(value);
  }

  @Override
  public void process_deposit(MutableBeaconState state, Deposit deposit) {
    delegate.process_deposit(state, deposit);
  }

  @Override
  public void process_deposit_inner(
      MutableBeaconState state, Deposit deposit, boolean verifyProof) {
    delegate.process_deposit_inner(state, deposit, verifyProof);
  }

  @Override
  public EpochNumber get_delayed_activation_exit_epoch(EpochNumber epoch) {
    return delegate.get_delayed_activation_exit_epoch(epoch);
  }

  @Override
  public void activate_validator(MutableBeaconState state, ValidatorIndex index, boolean genesis) {
    delegate.activate_validator(state, index, genesis);
  }

  @Override
  public void slash_validator(MutableBeaconState state, ValidatorIndex index) {
    delegate.slash_validator(state, index);
  }

  @Override
  public void initiate_validator_exit(MutableBeaconState state, ValidatorIndex index) {
    delegate.initiate_validator_exit(state, index);
  }

  @Override
  public void exit_validator(MutableBeaconState state, ValidatorIndex index) {
    delegate.exit_validator(state, index);
  }

  @Override
  public void prepare_validator_for_withdrawal(MutableBeaconState state, ValidatorIndex index) {
    delegate.prepare_validator_for_withdrawal(state, index);
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return delegate.hash_tree_root(object);
  }

  @Override
  public Hash32 signed_root(Object object) {
    return delegate.signed_root(object);
  }

  @Override
  public Hash32 get_active_index_root(BeaconState state, EpochNumber epoch) {
    return delegate.get_active_index_root(state, epoch);
  }

  @Override
  public Hash32 generate_seed(BeaconState state, EpochNumber epoch) {
    return delegate.generate_seed(state, epoch);
  }

  @Override
  public boolean bls_verify(
      BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return delegate.bls_verify(publicKey, message, signature, domain);
  }

  @Override
  public boolean bls_verify(
      PublicKey blsPublicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return delegate.bls_verify(blsPublicKey, message, signature, domain);
  }

  @Override
  public boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    return delegate.bls_verify_multiple(publicKeys, messages, signature, domain);
  }

  @Override
  public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return delegate.bls_aggregate_pubkeys(publicKeysBytes);
  }

  @Override
  public Bytes4 get_fork_version(Fork fork, EpochNumber epoch) {
    return delegate.get_fork_version(fork, epoch);
  }

  @Override
  public UInt64 get_domain(Fork fork, EpochNumber epoch, UInt64 domainType) {
    return delegate.get_domain(fork, epoch, domainType);
  }

  @Override
  public boolean is_double_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    return delegate.is_double_vote(attestation_data_1, attestation_data_2);
  }

  @Override
  public boolean is_surround_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    return delegate.is_surround_vote(attestation_data_1, attestation_data_2);
  }

  @Override
  public boolean verify_slashable_attestation(
      BeaconState state, SlashableAttestation slashable_attestation) {
    return delegate.verify_slashable_attestation(state, slashable_attestation);
  }

  @Override
  public boolean verify_bitfield(Bitfield bitfield, int committee_size) {
    return delegate.verify_bitfield(bitfield, committee_size);
  }

  @Override
  public Hash32 get_block_root(BeaconState state, SlotNumber slot) {
    return delegate.get_block_root(state, slot);
  }

  @Override
  public Hash32 get_state_root(BeaconState state, SlotNumber slot) {
    return delegate.get_state_root(state, slot);
  }

  @Override
  public List<ValidatorIndex> get_attestation_participants(
      BeaconState state, AttestationData attestation_data, Bitfield bitfield) {
    return delegate.get_attestation_participants(state, attestation_data, bitfield);
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    return delegate.get_validator_index_by_pubkey(state, pubkey);
  }

  @Override
  public SlotNumber get_current_slot(BeaconState state, long systemTime) {
    return delegate.get_current_slot(state, systemTime);
  }

  @Override
  public boolean is_current_slot(BeaconState state, long systemTime) {
    return delegate.is_current_slot(state, systemTime);
  }

  @Override
  public Time get_slot_start_time(BeaconState state, SlotNumber slot) {
    return delegate.get_slot_start_time(state, slot);
  }

  @Override
  public Time get_slot_middle_time(BeaconState state, SlotNumber slot) {
    return delegate.get_slot_middle_time(state, slot);
  }

  @Override
  public EpochNumber slot_to_epoch(SlotNumber slot) {
    return delegate.slot_to_epoch(slot);
  }

  @Override
  public EpochNumber get_previous_epoch(BeaconState state) {
    return delegate.get_previous_epoch(state);
  }

  @Override
  public EpochNumber get_current_epoch(BeaconState state) {
    return delegate.get_current_epoch(state);
  }

  @Override
  public SlotNumber get_epoch_start_slot(EpochNumber epoch) {
    return delegate.get_epoch_start_slot(epoch);
  }

  @Override
  public void checkIndexRange(BeaconState state, ValidatorIndex index) {
    delegate.checkIndexRange(state, index);
  }

  @Override
  public void checkIndexRange(BeaconState state, Iterable<ValidatorIndex> indices) {
    delegate.checkIndexRange(state, indices);
  }

  @Override
  public void checkShardRange(ShardNumber shard) {
    delegate.checkShardRange(shard);
  }

  @Override
  public List<BLSPubkey> mapIndicesToPubKeys(BeaconState state, Iterable<ValidatorIndex> indices) {
    return delegate.mapIndicesToPubKeys(state, indices);
  }

  @Override
  public BeaconBlock lmd_ghost(
      BeaconBlock startBlock,
      BeaconState state,
      Function<Hash32, Optional<BeaconBlock>> getBlock,
      Function<Hash32, List<BeaconBlock>> getChildrenBlocks,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation) {
    return delegate.lmd_ghost(
        startBlock, state, getBlock, getChildrenBlocks, get_latest_attestation);
  }

  @Override
  public Optional<BeaconBlock> get_latest_attestation_target(
      ValidatorRecord validatorRecord,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {
    return delegate.get_latest_attestation_target(
        validatorRecord, get_latest_attestation, getBlock);
  }

  @Override
  public UInt64 get_vote_count(
      BeaconState startState,
      BeaconBlock block,
      List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {
    return delegate.get_vote_count(startState, block, attestation_targets, getBlock);
  }

  @Override
  public Optional<BeaconBlock> get_ancestor(
      BeaconBlock block, SlotNumber slot, Function<Hash32, Optional<BeaconBlock>> getBlock) {
    return delegate.get_ancestor(block, slot, getBlock);
  }

  @Override
  public boolean is_epoch_end(SlotNumber slot) {
    return delegate.is_epoch_end(slot);
  }

  @Override
  public BeaconBlock get_empty_block() {
    return delegate.get_empty_block();
  }

  @Override
  public BeaconState get_genesis_beacon_state(
      List<Deposit> genesisValidatorDeposits, Time genesisTime, Eth1Data genesisEth1Data) {
    return delegate.get_genesis_beacon_state(
        genesisValidatorDeposits, genesisTime, genesisEth1Data);
  }

  @Override
  public void cache_state(MutableBeaconState state) {
    delegate.cache_state(state);
  }

  @Override
  public void advance_slot(MutableBeaconState state) {
    delegate.advance_slot(state);
  }

  @Override
  public Gwei get_current_total_balance(BeaconState state) {
    return delegate.get_current_total_balance(state);
  }

  @Override
  public Gwei get_previous_total_balance(BeaconState state) {
    return delegate.get_previous_total_balance(state);
  }

  @Override
  public List<ValidatorIndex> get_attesting_indices(
      BeaconState state, List<PendingAttestation> attestations) {
    return delegate.get_attesting_indices(state, attestations);
  }

  @Override
  public Gwei get_attesting_balance(BeaconState state, List<PendingAttestation> attestations) {
    return delegate.get_attesting_balance(state, attestations);
  }

  @Override
  public List<PendingAttestation> get_current_epoch_boundary_attestations(BeaconState state) {
    return delegate.get_current_epoch_boundary_attestations(state);
  }

  @Override
  public List<PendingAttestation> get_previous_epoch_boundary_attestations(BeaconState state) {
    return delegate.get_previous_epoch_boundary_attestations(state);
  }

  @Override
  public List<PendingAttestation> get_previous_epoch_matching_head_attestations(BeaconState state) {
    return delegate.get_previous_epoch_matching_head_attestations(state);
  }

  @Override
  public PendingAttestation earliest_attestation(BeaconState state, ValidatorIndex validatorIndex) {
    return delegate.earliest_attestation(state, validatorIndex);
  }

  @Override
  public SlotNumber inclusion_slot(BeaconState state, ValidatorIndex validatorIndex) {
    return delegate.inclusion_slot(state, validatorIndex);
  }

  @Override
  public SlotNumber inclusion_distance(BeaconState state, ValidatorIndex validatorIndex) {
    return delegate.inclusion_distance(state, validatorIndex);
  }

  @Override
  public void update_justification_and_finalization(MutableBeaconState state) {
    delegate.update_justification_and_finalization(state);
  }

  @Override
  public void maybe_reset_eth1_period(MutableBeaconState state) {
    delegate.maybe_reset_eth1_period(state);
  }

  @Override
  public Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    return delegate.get_base_reward(state, index);
  }

  @Override
  public Gwei get_inactivity_penalty(
      BeaconState state, ValidatorIndex index, EpochNumber epochsSinceFinality) {
    return delegate.get_inactivity_penalty(state, index, epochsSinceFinality);
  }

  @Override
  public Gwei[][] compute_normal_justification_and_finalization_deltas(BeaconState state) {
    return delegate.compute_normal_justification_and_finalization_deltas(state);
  }

  @Override
  public Gwei[][] compute_inactivity_leak_deltas(BeaconState state) {
    return delegate.compute_inactivity_leak_deltas(state);
  }

  @Override
  public Gwei[][] get_justification_and_finalization_deltas(BeaconState state) {
    return delegate.get_justification_and_finalization_deltas(state);
  }

  @Override
  public Gwei[][] get_crosslink_deltas(BeaconState state) {
    return delegate.get_crosslink_deltas(state);
  }

  @Override
  public void apply_rewards(MutableBeaconState state) {
    delegate.apply_rewards(state);
  }

  @Override
  public List<ValidatorIndex> process_ejections(MutableBeaconState state) {
    return delegate.process_ejections(state);
  }

  @Override
  public boolean should_update_validator_registry(BeaconState state) {
    return delegate.should_update_validator_registry(state);
  }

  @Override
  public void update_validator_registry(MutableBeaconState state) {
    delegate.update_validator_registry(state);
  }

  @Override
  public void update_registry_and_shuffling_data(MutableBeaconState state) {
    delegate.update_registry_and_shuffling_data(state);
  }

  @Override
  public void process_slashings(MutableBeaconState state) {
    delegate.process_slashings(state);
  }

  @Override
  public boolean eligible(BeaconState state, ValidatorIndex index) {
    return delegate.eligible(state, index);
  }

  @Override
  public void process_exit_queue(MutableBeaconState state) {
    delegate.process_exit_queue(state);
  }

  @Override
  public void finish_epoch_update(MutableBeaconState state) {
    delegate.finish_epoch_update(state);
  }

  @Override
  public void process_block_header(MutableBeaconState state, BeaconBlock block) {
    delegate.process_block_header(state, block);
  }

  @Override
  public void process_randao(MutableBeaconState state, BeaconBlock block) {
    delegate.process_randao(state, block);
  }

  @Override
  public void process_eth1_data(MutableBeaconState state, BeaconBlock block) {
    delegate.process_eth1_data(state, block);
  }

  @Override
  public void process_proposer_slashing(
      MutableBeaconState state, ProposerSlashing proposer_slashing) {
    delegate.process_proposer_slashing(state, proposer_slashing);
  }

  @Override
  public void process_attester_slashing(
      MutableBeaconState state, AttesterSlashing attester_slashing) {
    delegate.process_attester_slashing(state, attester_slashing);
  }

  @Override
  public void process_attestation(MutableBeaconState state, Attestation attestation) {
    delegate.process_attestation(state, attestation);
  }

  @Override
  public void process_voluntary_exit(MutableBeaconState state, VoluntaryExit exit) {
    delegate.process_voluntary_exit(state, exit);
  }

  @Override
  public void process_transfer(MutableBeaconState state, Transfer transfer) {
    delegate.process_transfer(state, transfer);
  }

  @Override
  public Pair<Hash32, List<ValidatorIndex>> get_winning_root_and_participants(
      BeaconState state, ShardNumber shard, EpochNumber epoch) {
    return delegate.get_winning_root_and_participants(state, shard, epoch);
  }

  @Override
  public List<Crosslink> get_epoch_crosslinks(BeaconState state, EpochNumber epoch) {
    return delegate.get_epoch_crosslinks(state, epoch);
  }

  @Override
  public List<Crosslink> merge_crosslinks(
      List<Crosslink> crosslinks_1, List<Crosslink> crosslinks_2) {
    return delegate.merge_crosslinks(crosslinks_1, crosslinks_2);
  }

  @Override
  public List<Crosslink> get_latest_crosslinks(BeaconState state) {
    return delegate.get_latest_crosslinks(state);
  }

  @Override
  public void apply_crosslinks(MutableBeaconState state, List<Crosslink> latest_crosslinks) {
    delegate.apply_crosslinks(state, latest_crosslinks);
  }
}
