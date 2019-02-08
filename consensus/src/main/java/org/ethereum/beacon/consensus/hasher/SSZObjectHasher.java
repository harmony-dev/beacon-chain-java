package org.ethereum.beacon.consensus.hasher;

import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZHashSerializers;
import org.ethereum.beacon.ssz.SSZSerializer;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An object hasher implementation using Tree Hash algorithm described in the spec.
 *
 * @param <H> a hash type.
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#tree-hash">Tree
 *     Hash</a> in the spec.
 */
public class SSZObjectHasher<H extends BytesValue> implements ObjectHasher<H> {

  private final SSZSerializer sszHashSerializer;
  private final Function<BytesValue, H> hashFunction;

  public static <H extends BytesValue> SSZObjectHasher<H> create(
      Function<BytesValue, H> hashFunction) {
    SSZSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(hashFunction, true);
    return new SSZObjectHasher<>(sszHashSerializer, hashFunction);
  }

  SSZObjectHasher(SSZSerializer sszHashSerializer, Function<BytesValue, H> hashFunction) {
    this.sszHashSerializer = sszHashSerializer;
    this.hashFunction = hashFunction;
  }

  @Override
  public H getHash(Object input) {
    return hashFunction.apply(BytesValue.wrap(sszHashSerializer.encode(input)));
  }
}
