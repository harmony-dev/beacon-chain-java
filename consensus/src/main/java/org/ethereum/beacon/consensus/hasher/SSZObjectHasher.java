package org.ethereum.beacon.consensus.hasher;

import org.ethereum.beacon.ssz.SSZHashSerializer;
import org.ethereum.beacon.ssz.SSZHashSerializers;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;
import java.util.function.Function;

/**
 * An object hasher implementation using Tree Hash algorithm described in the spec.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#tree-hash">Tree
 *     Hash</a> in the spec.
 */
public class SSZObjectHasher implements ObjectHasher<Hash32> {

  private static final int SSZ_SCHEMES_CACHE_CAPACITY = 128;
  private final SSZHashSerializer sszHashSerializer;
  private final Function<BytesValue, Hash32> hashFunction;

  SSZObjectHasher(SSZHashSerializer sszHashSerializer, Function<BytesValue, Hash32> hashFunction) {
    this.sszHashSerializer = sszHashSerializer;
    this.hashFunction = hashFunction;
  }

  /**
   * Creates object hasher with hash function and SSZ scheme builder with cache
   * @param hashFunction    Hash function
   * @return hasher
   */
  public static SSZObjectHasher create(Function<BytesValue, Hash32> hashFunction) {
    SSZHashSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(
            hashFunction, true, SSZ_SCHEMES_CACHE_CAPACITY);
    return new SSZObjectHasher(sszHashSerializer, hashFunction);
  }

  @Override
  public Hash32 getHash(Object input) {
    if (input instanceof List) {
      return Hash32.wrap(Bytes32.wrap(sszHashSerializer.hash(input)));
    } else {
      return hashFunction.apply(BytesValue.wrap(sszHashSerializer.hash(input)));
    }
  }

  @Override
  public Hash32 getHashTruncate(Object input, String field) {
    if (input instanceof List) {
      throw new RuntimeException("Lists are not supported in truncated hash");
    } else {
      return hashFunction.apply(
          BytesValue.wrap(sszHashSerializer.hashTruncate(input, input.getClass(), field)));
    }
  }
}
