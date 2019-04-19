package org.ethereum.beacon.bench;

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
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.util.stats.TimeCollector;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class BenchmarkingBeaconChainSpec extends CachingBeaconChainSpec {

  private final Map<String, TimeCollector> collectors = new ConcurrentHashMap<>();
  private boolean trackingStarted = false;

  static BenchmarkingBeaconChainSpec wrap(BeaconChainSpec spec) {
    return new BenchmarkingBeaconChainSpec(
        spec.getConstants(),
        spec.getHashFunction(),
        spec.getObjectHasher(),
        spec.isBlsVerify(),
        spec.isBlsVerifyProofOfPossession(),
        spec instanceof CachingBeaconChainSpec && ((CachingBeaconChainSpec) spec).isCacheEnabled());
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

  @Override
  public Hash32 hash_tree_root(Object object) {
    return callAndTrack("hash_tree_root", () -> super.hash_tree_root(object));
  }

  @Override
  public Hash32 signed_root(Object object) {
    return callAndTrack("signed_root", () -> super.signed_root(object));
  }

  @Override
  public List<ShardCommittee> get_crosslink_committees_at_slot(BeaconState state, SlotNumber slot) {
    return callAndTrack(
        "get_crosslink_committees_at_slot",
        () -> super.get_crosslink_committees_at_slot(state, slot));
  }

  @Override
  public ValidatorIndex get_beacon_proposer_index(BeaconState state, SlotNumber slot) {
    return callAndTrack(
        "get_beacon_proposer_index", () -> super.get_beacon_proposer_index(state, slot));
  }

  @Override
  public void update_justification_and_finalization(MutableBeaconState state) {
    callAndTrack(
        "update_justification_and_finalization",
        () -> super.update_justification_and_finalization(state));
  }

  @Override
  public void process_crosslinks(MutableBeaconState state) {
    callAndTrack("process_crosslinks", () -> super.process_crosslinks(state));
  }

  @Override
  public void maybe_reset_eth1_period(MutableBeaconState state) {
    callAndTrack("maybe_reset_eth1_period", () -> super.maybe_reset_eth1_period(state));
  }

  @Override
  public void apply_rewards(MutableBeaconState state) {
    callAndTrack("apply_rewards", () -> super.apply_rewards(state));
  }

  @Override
  public List<ValidatorIndex> process_ejections(MutableBeaconState state) {
    return callAndTrack("process_ejections", () -> super.process_ejections(state));
  }

  @Override
  public void update_registry_and_shuffling_data(MutableBeaconState state) {
    callAndTrack(
        "update_registry_and_shuffling_data",
        () -> super.update_registry_and_shuffling_data(state));
  }

  @Override
  public void process_slashings(MutableBeaconState state) {
    callAndTrack("process_slashings", () -> super.process_slashings(state));
  }

  @Override
  public void process_exit_queue(MutableBeaconState state) {
    callAndTrack("process_exit_queue", () -> super.process_exit_queue(state));
  }

  @Override
  public void finish_epoch_update(MutableBeaconState state) {
    callAndTrack("finish_epoch_update", () -> super.finish_epoch_update(state));
  }

  @Override
  public void cache_state(MutableBeaconState state) {
    callAndTrack("cache_state", () -> super.cache_state(state));
  }

  @Override
  public void advance_slot(MutableBeaconState state) {
    callAndTrack("advance_slot", () -> super.advance_slot(state));
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
  public void process_randao(MutableBeaconState state, BeaconBlock block) {
    callAndTrack(
        "process_randao",
        () -> {
          super.verify_randao(state, block);
          super.process_randao(state, block);
        });
  }

  @Override
  public void process_eth1_data(MutableBeaconState state, BeaconBlock block) {
    callAndTrack("process_eth1_data", () -> super.process_eth1_data(state, block));
  }

  void startTracking() {
    this.trackingStarted = true;
  }

  Map<String, TimeCollector> drainCollectors() {
    Map<String, TimeCollector> result = new HashMap<>(collectors);
    this.collectors.clear();
    return result;
  }

  private void callAndTrack(String name, Runnable routine) {
    if (!trackingStarted) {
      routine.run();
      return;
    }

    TimeCollector collector = collectors.computeIfAbsent(name, s -> new TimeCollector());
    long s = System.nanoTime();
    routine.run();
    collector.tick(System.nanoTime() - s);
  }

  private <T> T callAndTrack(String name, Supplier<T> routine) {
    if (!trackingStarted) {
      return routine.get();
    }

    TimeCollector collector = collectors.computeIfAbsent(name, s -> new TimeCollector());
    long s = System.nanoTime();
    T result = routine.get();
    collector.tick(System.nanoTime() - s);

    return result;
  }
}
