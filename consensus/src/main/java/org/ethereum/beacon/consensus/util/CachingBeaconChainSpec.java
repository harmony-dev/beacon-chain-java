package org.ethereum.beacon.consensus.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.ethereum.beacon.consensus.BeaconChainSpecImpl;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.util.cache.Cache;
import org.ethereum.beacon.util.cache.CacheFactory;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public class CachingBeaconChainSpec extends BeaconChainSpecImpl {

  private final Cache<Pair<List<? extends UInt64>, Bytes32>, List<UInt64>> shufflerCache;
  private final Cache<Object, Hash32> hashTreeRootCache;
  private final Cache<Object, Hash32> signedRootCache;
  private final Cache<SlotNumber, List<ShardCommittee>> crosslinkCommitteesCache;
  private final Cache<EpochNumber, List<ValidatorIndex>> activeValidatorsCache;
  private final Cache<EpochNumber, Gwei> totalBalanceCache;

  private ValidatorIndex maxCachedIndex = ValidatorIndex.ZERO;
  private final Map<BLSPubkey, ValidatorIndex> pubkeyToIndexCache = new ConcurrentHashMap<>();

  private final boolean cacheEnabled;

  public CachingBeaconChainSpec(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession,
      boolean cacheEnabled) {
    super(constants, hashFunction, objectHasher, blsVerify, blsVerifyProofOfPossession);
    this.cacheEnabled = cacheEnabled;

    CacheFactory factory = CacheFactory.create(cacheEnabled);
    this.shufflerCache = factory.createLRUCache(1024);
    this.hashTreeRootCache = factory.createLRUCache(1024);
    this.signedRootCache = factory.createLRUCache(1024);
    this.crosslinkCommitteesCache = factory.createLRUCache(128);
    this.activeValidatorsCache = factory.createLRUCache(32);
    this.totalBalanceCache = factory.createLRUCache(32);
  }

  public CachingBeaconChainSpec(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession) {
    this(constants, hashFunction, objectHasher, blsVerify, blsVerifyProofOfPossession, true);
  }

  @Override
  public List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    return shufflerCache.get(
        Pair.with(indices, seed),
        k -> super.get_permuted_list(k.getValue0(), k.getValue1()));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return hashTreeRootCache.get(object, super::hash_tree_root);
  }

  @Override
  public Hash32 signed_root(Object object) {
    return signedRootCache.get(object, super::signed_root);
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    if (!cacheEnabled) {
      return super.get_validator_index_by_pubkey(state, pubkey);
    }

    // relying on the fact that at index -> validator is invariant
    if (state.getValidatorRegistry().size().greater(maxCachedIndex)) {
      for (ValidatorIndex index : maxCachedIndex.iterateTo(state.getValidatorRegistry().size())) {
        pubkeyToIndexCache.put(state.getValidatorRegistry().get(index).getPubKey(), index);
      }
      maxCachedIndex = state.getValidatorRegistry().size();
    }
    return pubkeyToIndexCache.getOrDefault(pubkey, ValidatorIndex.MAX);
  }

  @Override
  public List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot, boolean registry_change) {
    return crosslinkCommitteesCache.get(
        slot, s -> super.get_crosslink_committees_at_slot(state, slot, registry_change));
  }

  @Override
  public List<ValidatorIndex> get_active_validator_indices(
      ReadList<ValidatorIndex, ValidatorRecord> validators, EpochNumber epochNumber) {
    return activeValidatorsCache.get(
        epochNumber, e -> super.get_active_validator_indices(validators, epochNumber));
  }

  @Override
  public Gwei get_previous_total_balance(BeaconState state) {
    return totalBalanceCache.get(
        get_previous_epoch(state), e -> super.get_previous_total_balance(state));
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }
}
