package org.ethereum.beacon.consensus.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.util.LRUCache;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class CachingSpecHelpers extends SpecHelpers {

  public static SpecHelpers createWithSSZHasher(@Nonnull SpecConstants constants) {
    Objects.requireNonNull(constants);

    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    ObjectHasher<Hash32> sszHasher = SSZObjectHasher.create(hashFunction);
    return new SpecHelpers(constants, hashFunction, sszHasher);
  }

  private final LRUCache<Pair<List<? extends UInt64>, Bytes32>, List<UInt64>> shufflerCache =
      new LRUCache<>(1024);
  private final LRUCache<Object, Hash32> hashTreeRootCache =
      new LRUCache<>(1024);
  private final LRUCache<Object, Hash32> signedRootCache =
      new LRUCache<>(1024);
  private ValidatorIndex maxCachedIndex = ValidatorIndex.ZERO;
  private final Map<BLSPubkey, ValidatorIndex> pubkeyToIndexCache = new ConcurrentHashMap<>();

  public CachingSpecHelpers(SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher) {
    super(constants, hashFunction, objectHasher);
  }

  public List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    return shufflerCache.get(
        Pair.with(indices, seed),
        k -> CachingSpecHelpers.super.get_permuted_list(k.getValue0(), k.getValue1()));
  }

  @Override
  public Hash32 hash_tree_root(Object object) {
    return hashTreeRootCache.get(object, CachingSpecHelpers.super::hash_tree_root);
  }

  @Override
  public Hash32 signed_root(Object object) {
    return signedRootCache.get(object, CachingSpecHelpers.super::signed_root);
  }

  @Override
  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    // relying on the fact that at index -> validator is invariant
    if (state.getValidatorRegistry().size().greater(maxCachedIndex)) {
      for (ValidatorIndex index : maxCachedIndex.iterateTo(state.getValidatorRegistry().size())) {
        pubkeyToIndexCache.put(state.getValidatorRegistry().get(index).getPubKey(), index);
      }
      maxCachedIndex = state.getValidatorRegistry().size();
    }
    return pubkeyToIndexCache.getOrDefault(pubkey, ValidatorIndex.MAX);
  }
}
