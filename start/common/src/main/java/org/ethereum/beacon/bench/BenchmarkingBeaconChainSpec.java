package org.ethereum.beacon.bench;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.util.CachingBeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.util.collections.Bitlist;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.util.stats.MeasurementsCollector;
import org.ethereum.beacon.util.stats.TimeCollector;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class BenchmarkingBeaconChainSpec extends CachingBeaconChainSpec {

  private final Map<String, MeasurementsCollector> collectors = new ConcurrentHashMap<>();
  private boolean trackingStarted = false;

  static BenchmarkingBeaconChainSpec wrap(BeaconChainSpec spec) {
    BenchmarkingBeaconChainSpec wrapped = new BenchmarkingBeaconChainSpec(
        spec.getConstants(),
        spec.getHashFunction(),
        spec.getObjectHasher(),
        spec.isBlsVerify(),
        spec.isBlsVerifyProofOfPossession(),
        spec instanceof CachingBeaconChainSpec && ((CachingBeaconChainSpec) spec).isCacheEnabled());

    // share caches between all instances to avoid cache duplication
    if (spec instanceof CachingBeaconChainSpec) {
      wrapped.caches = ((CachingBeaconChainSpec) spec).getCaches();
    }

    return wrapped;
  }

  public BenchmarkingBeaconChainSpec(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession,
      boolean cacheEnabled) {
    super(
        constants, hashFunction, objectHasher, blsVerify, blsVerifyProofOfPossession, cacheEnabled);
  }

  @Override
  public SpecConstants getConstants() {
    return super.getConstants();
  }

  @Override
  public ObjectHasher<Hash32> getObjectHasher() {
    return super.getObjectHasher();
  }

  @Override
  public Function<BytesValue, Hash32> getHashFunction() {
    return super.getHashFunction();
  }

  /** BLS */
  @Override
  public boolean bls_verify(
      BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return callAndTrack(
        "bls_verify", () -> super.bls_verify(publicKey, message, signature, domain));
  }

  @Override
  public boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    return callAndTrack(
        "bls_verify_multiple",
        () -> super.bls_verify_multiple(publicKeys, messages, signature, domain));
  }

  @Override
  public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return callAndTrack(
        "bls_aggregate_pubkeys", () -> super.bls_aggregate_pubkeys(publicKeysBytes));
  }

  /** HELPERS */
  @Override
  public Hash32 hash_tree_root(Object object) {
    return callAndTrack("hash_tree_root", () -> super.hash_tree_root(object));
  }

  @Override
  public Hash32 signing_root(Object object) {
    return callAndTrack("signing_root", () -> super.signing_root(object));
  }

  @Override
  public List<ValidatorIndex> get_crosslink_committee(
      BeaconState state, EpochNumber epoch, ShardNumber shard) {
    return callAndTrack(
        "get_crosslink_committee", () -> super.get_crosslink_committee(state, epoch, shard));
  }

  @Override
  public ValidatorIndex get_beacon_proposer_index(BeaconState state) {
    return callAndTrack("get_beacon_proposer_index", () -> super.get_beacon_proposer_index(state));
  }

  @Override
  public List<ValidatorIndex> get_active_validator_indices(
      BeaconState state, EpochNumber epochNumber) {
    return callAndTrack(
        "get_active_validator_indices",
        () -> super.get_active_validator_indices(state, epochNumber));
  }

  @Override
  public Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> validators) {
    return callAndTrack("get_total_balance", () -> super.get_total_balance(state, validators));
  }

  @Override
  public UInt64 get_validator_churn_limit(BeaconState state) {
    return callAndTrack("get_validator_churn_limit", () -> super.get_validator_churn_limit(state));
  }

  @Override
  public List<ValidatorIndex> get_attesting_indices(
      BeaconState state, AttestationData attestation_data, Bitlist bitlist) {
    return callAndTrack(
        "get_attesting_indices",
        () -> super.get_attesting_indices(state, attestation_data, bitlist));
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    return callAndTrack(
        "get_validator_index_by_pubkey", () -> super.get_validator_index_by_pubkey(state, pubkey));
  }

  @Override
  public Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    return callAndTrack("get_base_reward", () -> super.get_base_reward(state, index));
  }

  @Override
  public boolean is_valid_indexed_attestation(
      BeaconState state, IndexedAttestation indexed_attestation) {
    return callAndTrack(
        "is_valid_indexed_attestation",
        () -> super.is_valid_indexed_attestation(state, indexed_attestation));
  }

  @Override
  public List<ValidatorIndex> get_unslashed_attesting_indices(
      BeaconState state, List<PendingAttestation> attestations) {
    return callAndTrack(
        "get_unslashed_attesting_indices",
        () -> super.get_unslashed_attesting_indices(state, attestations));
  }

  @Override
  public Hash32 get_seed(BeaconState state, EpochNumber epoch) {
    return callAndTrack("get_seed", () -> super.get_seed(state, epoch));
  }

  @Override
  public Pair<Crosslink, List<ValidatorIndex>> get_winning_crosslink_and_attesting_indices(
      BeaconState state, EpochNumber epoch, ShardNumber shard) {
    return callAndTrack(
        "get_winning_crosslink_and_attesting_indices",
        () -> super.get_winning_crosslink_and_attesting_indices(state, epoch, shard));
  }

  @Override
  public UInt64 get_shard_delta(BeaconState state, EpochNumber epoch) {
    return callAndTrack("get_shard_delta", () -> super.get_shard_delta(state, epoch));
  }

  @Override
  public ShardNumber get_start_shard(BeaconState state, EpochNumber epoch) {
    return callAndTrack("get_start_shard", () -> super.get_start_shard(state, epoch));
  }

  @Override
  public SlotNumber get_attestation_data_slot(BeaconState state, AttestationData data) {
    return callAndTrack("get_attestation_data_slot", () -> super.get_attestation_data_slot(state, data));
  }

  @Override
  public UInt64 get_committee_count(BeaconState state, EpochNumber epoch) {
    return callAndTrack(
        "get_committee_count", () -> super.get_committee_count(state, epoch));
  }

  @Override
  public Gwei get_total_active_balance(BeaconState state) {
    return callAndTrack("get_total_active_balance", () -> super.get_total_active_balance(state));
  }

  @Override
  public Gwei get_attesting_balance(BeaconState state, List<PendingAttestation> attestations) {
    return callAndTrack(
        "get_attesting_balance", () -> super.get_attesting_balance(state, attestations));
  }

  /** SLOT PROCESSING */
  @Override
  public void process_slot(MutableBeaconState state) {
    callAndTrack("process_slot", () -> super.process_slot(state));
  }

  /** EPOCH PROCESSING */
  @Override
  public void process_justification_and_finalization(MutableBeaconState state) {
    callAndTrack(
        "process_justification_and_finalization",
        () -> super.process_justification_and_finalization(state));
  }

  @Override
  public void process_crosslinks(MutableBeaconState state) {
    callAndTrack("process_crosslinks", () -> super.process_crosslinks(state));
  }

  @Override
  public void process_rewards_and_penalties(MutableBeaconState state) {
    callAndTrack("process_rewards_and_penalties", () -> super.process_rewards_and_penalties(state));
  }

  @Override
  public List<ValidatorIndex> process_registry_updates(MutableBeaconState state) {
    return callAndTrack("process_registry_updates", () -> super.process_registry_updates(state));
  }

  @Override
  public void process_slashings(MutableBeaconState state) {
    callAndTrack("process_slashings", () -> super.process_slashings(state));
  }

  @Override
  public void process_final_updates(MutableBeaconState state) {
    callAndTrack("process_final_updates", () -> super.process_final_updates(state));
  }

  /** BLOCK PROCESSING */
  @Override
  public void process_block_header(MutableBeaconState state, BeaconBlock block) {
    callAndTrack(
        "process_block_header",
        () -> {
          super.verify_block_header(state, block);
          super.process_block_header(state, block);
        });
  }

  @Override
  public void process_randao(MutableBeaconState state, BeaconBlockBody body) {
    callAndTrack(
        "process_randao",
        () -> {
          super.verify_randao(state, body);
          super.process_randao(state, body);
        });
  }

  @Override
  public void process_eth1_data(MutableBeaconState state, BeaconBlockBody body) {
    callAndTrack("process_eth1_data", () -> super.process_eth1_data(state, body));
  }

  @Override
  public void process_attestation(MutableBeaconState state, Attestation attestation) {
    callAndTrack(
        "process_attestation",
        () -> {
          // count verification in benchmark measurements
          super.verify_attestation(state, attestation);
          super.process_attestation(state, attestation);
        });
  }

  void startTracking() {
    this.trackingStarted = true;
  }

  Map<String, MeasurementsCollector> drainCollectors() {
    Map<String, MeasurementsCollector> result = new HashMap<>(collectors);
    this.collectors.clear();
    return result;
  }

  private void callAndTrack(String name, Runnable routine) {
    if (!trackingStarted) {
      routine.run();
      return;
    }

    TimeCollector collector = collectors.computeIfAbsent(name, s -> new MeasurementsCollector());
    long s = System.nanoTime();
    routine.run();
    collector.tick(System.nanoTime() - s);
  }

  private <T> T callAndTrack(String name, Supplier<T> routine) {
    if (!trackingStarted) {
      return routine.get();
    }

    TimeCollector collector = collectors.computeIfAbsent(name, s -> new MeasurementsCollector());
    long s = System.nanoTime();
    T result = routine.get();
    collector.tick(System.nanoTime() - s);

    return result;
  }
}
