package org.ethereum.beacon.consensus.hasher;

import java.util.function.Consumer;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.ssz.SSZHashSerializer;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;
import java.util.Optional;
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

  SSZObjectHasher(SSZHashSerializer sszHashSerializer) {
    this.sszHashSerializer = sszHashSerializer;
  }

  public static SSZObjectHasher create(Function<BytesValue, Hash32> hashFunction) {
    SSZHashSerializer sszHashSerializer = null; // TODO
    return new SSZObjectHasher(sszHashSerializer);
  }

  @Override
  public Hash32 getHash(Object input) {
    Function<Object, Hash32> hasher = o -> Hash32.wrap(Bytes32.wrap(sszHashSerializer.hash(o)));
    if (input instanceof Hashable) {
      return ((Hashable<Hash32>) input).getHash(hasher);
    } else {
      return hasher.apply(input);
    }
  }

  @Override
  public Hash32 getHashTruncate(Object input, String field) {
    if (input instanceof List) {
      throw new RuntimeException("Lists are not supported in truncated hash");
    } else {
      return Hash32.wrap(Bytes32.wrap(sszHashSerializer.hashTruncate(input, input.getClass(), field)));
    }
  }
}
