package org.ethereum.beacon.bench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.util.stats.TimeCollector;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class BenchSpecWrapper implements BeaconChainSpec {

  private final BeaconChainSpec delegate;

  private final Map<String, TimeCollector> collectors = new ConcurrentHashMap<>();
  private boolean trackingStarted = false;

  public BenchSpecWrapper(BeaconChainSpec delegate) {
    this.delegate = delegate;
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
  public boolean bls_verify(
      BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return callAndTrack(
        "bls_verify", () -> delegate.bls_verify(publicKey, message, signature, domain));
  }

  @Override
  public boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    return callAndTrack(
        "bls_verify_multiple",
        () -> delegate.bls_verify_multiple(publicKeys, messages, signature, domain));
  }

  @Override
  public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return callAndTrack(
        "bls_aggregate_pubkeys", () -> delegate.bls_aggregate_pubkeys(publicKeysBytes));
  }

  @Override
  public void process_deposit(MutableBeaconState state, Deposit deposit) {
    callAndTrack("process_deposit", () -> delegate.process_deposit(state, deposit));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return callAndTrack("hash_tree_root", () -> delegate.hash_tree_root(object));
  }

  @Override
  public Hash32 signed_root(Object object) {
    return callAndTrack("signed_root", () -> delegate.signed_root(object));
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    return callAndTrack(
        "get_validator_index_by_pubkey",
        () -> delegate.get_validator_index_by_pubkey(state, pubkey));
  }

  @Override
  public List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    return callAndTrack("get_permuted_list", () -> delegate.get_permuted_list(indices, seed));
  }

  @Override
  public void update_justification_and_finalization(MutableBeaconState state) {
    callAndTrack("update_justification_and_finalization", () -> delegate.update_justification_and_finalization(state));
  }

  @Override
  public void process_crosslinks(MutableBeaconState state) {
    callAndTrack("process_crosslinks", () -> delegate.process_crosslinks(state));
  }

  @Override
  public void maybe_reset_eth1_period(MutableBeaconState state) {
    callAndTrack("maybe_reset_eth1_period", () -> delegate.maybe_reset_eth1_period(state));
  }

  @Override
  public void apply_rewards(MutableBeaconState state) {
    callAndTrack("apply_rewards", () -> delegate.apply_rewards(state));
  }

  @Override
  public List<ValidatorIndex> process_ejections(MutableBeaconState state) {
    return callAndTrack("process_ejections", () -> delegate.process_ejections(state));
  }

  @Override
  public void update_registry_and_shuffling_data(MutableBeaconState state) {
    callAndTrack("update_registry_and_shuffling_data", () -> delegate.update_registry_and_shuffling_data(state));
  }

  @Override
  public void process_slashings(MutableBeaconState state) {
    callAndTrack("process_slashings", () -> delegate.process_slashings(state));
  }

  @Override
  public void process_exit_queue(MutableBeaconState state) {
    callAndTrack("process_exit_queue", () -> delegate.process_exit_queue(state));
  }

  @Override
  public void finish_epoch_update(MutableBeaconState state) {
    callAndTrack("finish_epoch_update", () -> delegate.finish_epoch_update(state));
  }

  @Override
  public void cache_state(MutableBeaconState state) {
    callAndTrack("cache_state", () -> delegate.cache_state(state));
  }

  @Override
  public void advance_slot(MutableBeaconState state) {
    callAndTrack("advance_slot", () -> delegate.advance_slot(state));
  }

  @Override
  public void process_attestation(MutableBeaconState state, Attestation attestation) {
    callAndTrack("process_attestation", () -> {
      // count verification in benchmark measurements
      delegate.verify_attestation(state, attestation);
      delegate.process_attestation(state, attestation);
    });
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
