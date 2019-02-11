package org.ethereum.beacon.consensus.hasher;

import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZHashSerializers;
import org.ethereum.beacon.ssz.SSZSerializer;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An object hasher implementation using Tree Hash algorithm described in the spec.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#tree-hash">Tree
 *     Hash</a> in the spec.
 */
public class SSZObjectHasher implements ObjectHasher<Hash32> {

  private final SSZSerializer sszHashSerializer;
  private final Function<BytesValue, Hash32> hashFunction;

  public static SSZObjectHasher create(Function<BytesValue, Hash32> hashFunction) {
    SSZSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(hashFunction, true);
    return new SSZObjectHasher(sszHashSerializer, hashFunction);
  }

  SSZObjectHasher(SSZSerializer sszHashSerializer, Function<BytesValue, Hash32> hashFunction) {
    this.sszHashSerializer = sszHashSerializer;
    this.hashFunction = hashFunction;
  }

  @Override
  public Hash32 getHash(Object input) {
    if (input instanceof List) {
      return Hash32.wrap(Bytes32.wrap(sszHashSerializer.encode(input)));
    } else {
      return hashFunction.apply(BytesValue.wrap(sszHashSerializer.encode(input)));
    }
  }
}
