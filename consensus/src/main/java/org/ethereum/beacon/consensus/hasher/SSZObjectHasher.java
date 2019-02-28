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

  private final SSZHashSerializer sszHashSerializer;

  SSZObjectHasher(SSZHashSerializer sszHashSerializer) {
    this.sszHashSerializer = sszHashSerializer;
  }

  public static SSZObjectHasher create(Function<BytesValue, Hash32> hashFunction) {
    SSZHashSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(hashFunction, true);
    return new SSZObjectHasher(sszHashSerializer);
  }

  @Override
  public Hash32 getHash(Object input) {
    return Hash32.wrap(Bytes32.wrap(sszHashSerializer.hash(input)));
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
