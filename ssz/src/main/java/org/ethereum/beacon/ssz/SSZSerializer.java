package org.ethereum.beacon.ssz;

import static org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.creator.SSZModelFactory;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer;
import org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer.DecodeResult;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHandler;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** SSZ serializer/deserializer */
public class SSZSerializer implements BytesSerializer, SSZVisitorHandler<SSZSimpleSerializer.SSZSerializerResult> {

  public static final int LENGTH_PREFIX_BYTE_SIZE = Integer.SIZE / Byte.SIZE;
  static final byte[] EMPTY_PREFIX = new byte[LENGTH_PREFIX_BYTE_SIZE];

  private SSZSchemeBuilder schemeBuilder;

  private SSZCodecResolver codecResolver;

  private SSZModelFactory sszModelFactory;

  private final SSZVisitorHost sszVisitorHost;
  private final SSZSimpleSerializer simpleSerializer;
  private TypeResolver typeResolver;

  /**
   * SSZ serializer/deserializer with following helpers
   *
   * @param schemeBuilder SSZ model scheme building of type {@link SSZScheme}
   * @param codecResolver Resolves field encoder/decoder {@link
   *     org.ethereum.beacon.ssz.access.SSZCodec} function
   * @param sszModelFactory Instantiates SSZModel with field/data information
   */
  public SSZSerializer(
      SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver,
      SSZModelFactory sszModelFactory,
      TypeResolver typeResolver) {
    this.schemeBuilder = schemeBuilder;
    this.codecResolver = codecResolver;
    this.sszModelFactory = sszModelFactory;
    this.typeResolver = typeResolver;
    sszVisitorHost = new SSZVisitorHost();
    simpleSerializer = new SSZSimpleSerializer();
  }

  static void checkSSZSerializableAnnotation(Class clazz) {
    if (!clazz.isAnnotationPresent(SSZSerializable.class)) {
      String error =
          String.format(
              "Serializer doesn't know how to handle class of type %s. Maybe you forget to "
                  + "annotate it with SSZSerializable?",
              clazz);
      throw new SSZSchemeException(error);
    }
  }

  /**
   * Serializes input to byte[] data
   *
   * @param inputObject input value
   * @param inputClazz Class of value
   * @return SSZ serialization
   */
  @Override
  public <C> byte[] encode(@Nullable C inputObject, Class<? extends C> inputClazz) {
    return visit(inputObject, inputClazz).getSerialized().getArrayUnsafe();
  }

  @Override
  public <C> SSZSerializerResult visit(C input, Class<? extends C> clazz) {
    return visitAny(typeResolver.resolveSSZType(new SSZField(clazz)), input);
  }

  @Override
  public SSZSerializerResult visitAny(SSZType sszType, Object value) {
    return sszVisitorHost.handleAny(sszType, value, new SSZSimpleSerializer());
  }

  /**
   * Restores data instance from serialization data using {@link SSZModelFactory}
   *
   * @param data SSZ serialization byte[] data
   * @param clazz type class
   * @return deserialized instance of clazz or throws exception
   */
  public <C> C decode(byte[] data, Class<? extends C> clazz) {
    DecodeResult decodeResult = sszVisitorHost.handleAny(
        typeResolver.resolveSSZType(new SSZField(clazz)),
        BytesValue.wrap(data),
        new SSZSimpleDeserializer());
    return (C) decodeResult.decodedInstance;
  }
}
