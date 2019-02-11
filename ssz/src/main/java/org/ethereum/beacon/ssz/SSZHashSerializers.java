package org.ethereum.beacon.ssz;

import java.util.function.Function;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.type.BytesCodec;
import org.ethereum.beacon.ssz.type.HashCodec;
import org.ethereum.beacon.ssz.type.UIntCodec;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * SSZ Hashing helper made according to the following specs: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 * Tree Hash</a>
 *
 * <p>It's based on {@link SSZSerializer}, so for setup help, check its Javadoc and {@link
 * SSZSerializerBuilder} documentation
 */
public abstract class SSZHashSerializers {
  private SSZHashSerializers() {}

  /**
   * Creates an instance of {@link SSZHashSerializer} that able to serialize data types used by
   * Beacon Chain implementation.
   *
   * @param hashFunction a basic hash function that serializer does use.
   * @param explicitFieldAnnotation whether object fields must be annotated with {@link SSZ} to be
   *     picked by returned serializer.
   * @return serializer instance.
   */
  public static SSZSerializer createWithBeaconChainTypes(
      Function<BytesValue, ? extends BytesValue> hashFunction, boolean explicitFieldAnnotation) {
    SSZCodecHasher hashCodecResolver = SSZCodecHasher.createWithHashFunction(hashFunction);
    SSZSerializerBuilder builder =
        new SSZSerializerBuilder()
            .withSSZSchemeBuilder(new SSZAnnotationSchemeBuilder(explicitFieldAnnotation))
            .withDefaultSSZModelFactory()
            .withSSZCodecResolver(hashCodecResolver)
            .addPrimitivesCodecs()
            .addCodec(new UIntCodec())
            .addCodec(new HashCodec())
            .addCodec(new BytesCodec());

    return builder.buildCustom(
        objects -> {
          SSZSchemeBuilder schemeBuilder = objects.getValue0();
          SSZCodecResolver codecResolver = objects.getValue1();
          SSZModelFactory modelFactory = objects.getValue2();
          return new SSZHashSerializer(schemeBuilder, codecResolver, modelFactory);
        });
  }
}
