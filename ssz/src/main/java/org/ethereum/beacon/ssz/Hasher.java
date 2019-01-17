package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.ConstructorObjCreator;
import org.ethereum.beacon.ssz.SSZAnnotationSchemeBuilder;
import org.ethereum.beacon.ssz.SSZCodecHasher;
import org.ethereum.beacon.ssz.SSZCodecResolver;
import org.ethereum.beacon.ssz.SSZHashSerializer;
import org.ethereum.beacon.ssz.SSZModelCreator;
import org.ethereum.beacon.ssz.SSZModelFactory;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.SSZSerializerBuilder;
import org.ethereum.beacon.ssz.SettersObjCreator;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.util.function.Function;

/**
 * <p>SSZ Hashing helper made according to the following specs:
 * <a href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ Tree Hash</a></p>
 *
 * <p>It's similar to {@link org.ethereum.beacon.ssz.SSZHasher}, but made to work with BytesValue</p>
 */
public class Hasher {
  private SSZSerializer hasher;

  private Function<BytesValue, BytesValue> hashFunction;

  /**
   * Creates instance of SSZ Hasher with
   * @param builder        Almost baked builder, without {@link SSZSerializerBuilder#build()} method being called
   * @param hashFunction   Function that will be used for hashing
   */
  public Hasher(SSZSerializerBuilder builder, Function<BytesValue, BytesValue> hashFunction) {
    this.hashFunction = hashFunction;
    this.hasher = builder.build();
  }

  /**
   * Builder for SSZHasher with primitive java types support
   * @param hashFunction          Function that will be used for hashing
   * @param explicitAnnotations   Whether to require explicit SSZ annotations at each field
   * @return prebaked SSZHasher builder
   */
  public static SSZSerializerBuilder getDefaultBuilder(Function<BytesValue, BytesValue> hashFunction, boolean explicitAnnotations) {
    SSZSerializerBuilder builder = new SSZSerializerBuilder();
    SSZCodecResolver hasher = new SSZCodecHasher(new Function<Bytes, Bytes>() {
      @Override
      public Bytes apply(Bytes bytes) {
        BytesValue input = BytesValue.of(bytes.toArrayUnsafe());
        return Bytes.wrap(hashFunction.apply(input).getArrayUnsafe());
      }
    });
    SSZSerializer sszSerializer = new SSZHashSerializer(
        new SSZAnnotationSchemeBuilder(explicitAnnotations),
        hasher, createDefaultModelCreator()
    );
    builder.initWith(sszSerializer, hasher);
    builder.addPrimitivesCodecs();

    return builder;
  }

  private static SSZModelFactory createDefaultModelCreator() {
    return new SSZModelCreator()
        .registerObjCreator(new ConstructorObjCreator())
        .registerObjCreator(new SettersObjCreator());
  }

  /**
   * Calcs SSZ Hash for provided SSZ model instance
   * @param input   SSZ model instance
   * @return SSZ Hash, 32 bytes wrapped with Bytes type
   */
  public BytesValue calc(Object input) {
    return hashFunction.apply(BytesValue.of(hasher.encode(input)));
  }
}
