package org.ethereum.beacon.consensus.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.ethereum.beacon.consensus.BeaconChainSpecImpl;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.util.LRUCache;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class CachingBeaconChainSpec extends BeaconChainSpecImpl {

  private final LRUCache<Pair<List<? extends UInt64>, Bytes32>, List<UInt64>> shufflerCache =
      new LRUCache<>(1024);
  private final LRUCache<Object, Hash32> hashTreeRootCache =
      new LRUCache<>(1024);
  private final LRUCache<Object, Hash32> signedRootCache =
      new LRUCache<>(1024);
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
    if (!cacheEnabled) {
      return super.get_permuted_list(indices, seed);
    }

    return shufflerCache.get(
        Pair.with(indices, seed),
        k -> super.get_permuted_list(k.getValue0(), k.getValue1()));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    if (!cacheEnabled) {
      return super.hash_tree_root(object);
    }

    return hashTreeRootCache.get(object, super::hash_tree_root);
  }

  @Override
  public Hash32 signed_root(Object object) {
    if (!cacheEnabled) {
      return super.signed_root(object);
    }

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

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }
}
