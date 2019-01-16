package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import java.util.function.Function;

/**
 * <p>SSZ Hashing helper made according to the following specs:
 * <a href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ Tree Hash</a></p>
 *
 * <p>It's based on {@link SSZSerializer}, so for setup help, check its Javadoc
 * and {@link SSZSerializerBuilder} documentation</p>
 */
public class SSZHasher {
  private SSZSerializer hasher;

  private Function<Bytes, Bytes> hashFunction;

  /**
   * Creates instance of SSZ Hasher with
   * @param builder        Almost baked builder, without {@link SSZSerializerBuilder#build()} method being called
   * @param hashFunction   Function that will be used for hashing
   */
  public SSZHasher(SSZSerializerBuilder builder, Function<Bytes, Bytes> hashFunction) {
    this.hashFunction = hashFunction;
    this.hasher = builder.build();
  }

  /**
   * Builder for SSZHasher with primitive java types support
   * @param hashFunction          Function that will be used for hashing
   * @param explicitAnnotations   Whether to require explicit SSZ annotations at each field
   * @return prebaked SSZHasher builder
   */
  public static SSZSerializerBuilder getDefaultBuilder(Function<Bytes, Bytes> hashFunction, boolean explicitAnnotations) {
    SSZSerializerBuilder builder = new SSZSerializerBuilder();
    SSZCodecResolver hasher = new SSZCodecHasher(hashFunction);
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
  public Bytes calc(Object input) {
    return hashFunction.apply(Bytes.wrap(hasher.encode(input)));
  }
}
