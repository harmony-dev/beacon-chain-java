package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.type.BytesCodec;
import org.ethereum.beacon.ssz.type.HashCodec;
import org.ethereum.beacon.ssz.type.UIntCodec;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.util.List;
import java.util.function.Function;

/**
 * SSZ Hashing helper made according to the following specs: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 * Tree Hash</a>
 *
 * <p>It's based on {@link SSZSerializer}, so for setup help, check its Javadoc and {@link
 * SSZSerializerBuilder} documentation
 */
public class SSZHasher implements Hasher<BytesValue> {
  private SSZSerializer hasher;

  private Function<BytesValue, BytesValue> hashFunction;

  /**
   * Creates instance of SSZ Hasher with
   *
   * @param builder Almost baked builder, without {@link SSZSerializerBuilder#build()} method being
   *     called
   * @param hashFunction Function that will be used for hashing
   */
  public SSZHasher(SSZSerializerBuilder builder, Function<BytesValue, BytesValue> hashFunction) {
    this.hashFunction = hashFunction;
    this.hasher =
        builder.buildCustom(
            objects -> {
              SSZSchemeBuilder schemeBuilder = objects.getValue0();
              SSZCodecResolver codecResolver = objects.getValue1();
              SSZModelFactory modelFactory = objects.getValue2();
              return new SSZHashSerializer(schemeBuilder, codecResolver, modelFactory);
            });
  }

  /**
   * Builder for SSZHasher with primitive java types support
   *
   * @param hashFunction Function that will be used for hashing
   * @param explicitAnnotations Whether to require explicit SSZ annotations at each field
   * @return prebaked SSZHasher builder
   */
  public static SSZSerializerBuilder getDefaultBuilder(
      Function<BytesValue, BytesValue> hashFunction, boolean explicitAnnotations) {
    SSZCodecResolver hasher =
        new SSZCodecHasher(
            bytes -> {
              BytesValue input = BytesValue.of(bytes.toArrayUnsafe());
              return Bytes.wrap(hashFunction.apply(input).getArrayUnsafe());
            });
    SSZSerializerBuilder builder =
        new SSZSerializerBuilder()
            .withSSZSchemeBuilder(new SSZAnnotationSchemeBuilder(explicitAnnotations))
            .withDefaultSSZModelFactory()
            .withSSZCodecResolver(hasher)
            .addPrimitivesCodecs();

    return builder;
  }

  /**
   * Mostly used hasher with all codecs included and required SSZ annotations for each field
   * @param hashingFunction BytesValue data hashing function
   * @return object hasher
   */
  public static Hasher<Hash32> simpleHasher(Function<BytesValue, BytesValue> hashingFunction) {
    Hasher<Hash32> objectHasher = new Hasher<Hash32>() {
      private final SSZHasher sszHasher =
          new SSZHasher(
              SSZHasher.getDefaultBuilder(hashingFunction, true)
                  .addCodec(new UIntCodec())
                  .addCodec(new HashCodec())
                  .addCodec(new BytesCodec()),
              hashingFunction);
      @Override
      public Hash32 calc(Object input) {
        return Hash32.wrap(Bytes32.wrap(sszHasher.calc(input), 0));
      }

      @Override
      public Hash32 calcList(List<Object> input) {
        return Hash32.wrap(Bytes32.wrap(sszHasher.calcList(input), 0));
      }
    };

    return objectHasher;
  }

  /**
   * Calcs SSZ Hash for provided SSZ model instance
   *
   * @param input SSZ model instance
   * @return SSZ Hash, 32 bytes wrapped with Bytes type
   */
  @Override
  public BytesValue calc(Object input) {
    return hashFunction.apply(BytesValue.wrap(hasher.encode(input)));
  }

  /**
   * Calcs SSZ Hash for provided SSZ list
   *
   * @param input SSZ list
   * @return SSZ Hash, 32 bytes wrapped with Bytes type
   */
  @Override
  public BytesValue calcList(List<Object> input) {
    return BytesValue.wrap(hasher.encode(input));
  }
}
