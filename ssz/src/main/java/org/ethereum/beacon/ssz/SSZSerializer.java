package org.ethereum.beacon.ssz;

import javax.annotation.Nullable;
import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.creator.CompositeObjCreator;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer;
import org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer.DecodeResult;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHandler;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;

/** SSZ serializer/deserializer */
public class SSZSerializer implements BytesSerializer, SSZVisitorHandler<SSZSimpleSerializer.SSZSerializerResult> {

  private final SSZVisitorHost sszVisitorHost;
  private final TypeResolver typeResolver;

  public SSZSerializer(SSZVisitorHost sszVisitorHost,
      TypeResolver typeResolver) {
    this.sszVisitorHost = sszVisitorHost;
    this.typeResolver = typeResolver;
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

  private <C> SSZSerializerResult visit(C input, Class<? extends C> clazz) {
    return visitAny(typeResolver.resolveSSZType(new SSZField(clazz)), input);
  }

  @Override
  public SSZSerializerResult visitAny(SSZType sszType, Object value) {
    return sszVisitorHost.handleAny(sszType, value, new SSZSimpleSerializer());
  }

  /**
   * Restores data instance from serialization data using {@link CompositeObjCreator}
   *
   * @param data SSZ serialization byte[] data
   * @param clazz type class
   * @return deserialized instance of clazz or throws exception
   */
  public <C> C decode(byte[] data, Class<? extends C> clazz) {
    DecodeResult decodeResult = sszVisitorHost.handleAny(
        typeResolver.resolveSSZType(new SSZField(clazz)),
        Bytes.wrap(data),
        new SSZSimpleDeserializer());
    if (data.length > decodeResult.readBytes) {
      throw new SSZSerializeException("Invalid SSZ data length (data is bigger than required): "
          + data.length + " > " + decodeResult.readBytes);
    }
    return (C) decodeResult.decodedInstance;
  }
}
