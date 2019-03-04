package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.ssz.type.BytesCodec;
import org.ethereum.beacon.ssz.type.BytesPrimitive;
import org.ethereum.beacon.ssz.type.HashCodec;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.StringPrimitive;
import org.ethereum.beacon.ssz.type.UIntCodec;
import org.ethereum.beacon.ssz.type.UIntPrimitive;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.annotation.Nullable;
import java.util.function.Function;

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
   * @param schemeBuilderCache  number of classes to keep built scheme in cache
   * @return serializer instance.
   */
  public static SSZHashSerializer createWithBeaconChainTypes(
      Function<BytesValue, ? extends BytesValue> hashFunction, boolean explicitFieldAnnotation,
      @Nullable Integer schemeBuilderCache) {
    SSZCodecHasher hashCodecResolver = SSZCodecHasher.createWithHashFunction(hashFunction);
    registerCodec(hashCodecResolver, new UIntPrimitive());
    registerCodec(hashCodecResolver, new BytesPrimitive());
    registerCodec(hashCodecResolver, new BooleanPrimitive());
    registerCodec(hashCodecResolver, new StringPrimitive());
    registerCodec(hashCodecResolver, new UIntCodec());
    registerCodec(hashCodecResolver, new HashCodec());
    registerCodec(hashCodecResolver, new BytesCodec());
    SSZAnnotationSchemeBuilder schemeBuilder = new SSZAnnotationSchemeBuilder(explicitFieldAnnotation);
    if (schemeBuilderCache != null) {
      schemeBuilder.withCache(schemeBuilderCache);
    }

    return new SSZHashSerializer(schemeBuilder, hashCodecResolver);
  }

  private static SSZCodecHasher registerCodec(SSZCodecHasher codecResolver, SSZCodec codec) {
    codecResolver.registerCodec(codec.getSupportedClasses(), codec.getSupportedSSZTypes(), codec);
    return codecResolver;
  }
}
