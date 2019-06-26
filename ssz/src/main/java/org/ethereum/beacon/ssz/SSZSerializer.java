package org.ethereum.beacon.ssz;

import javax.annotation.Nullable;
import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.creator.CompositeObjCreator;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHandler;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;
import org.ethereum.beacon.ssz.visitor.SosDeserializer;
import org.ethereum.beacon.ssz.visitor.SosSerializer;
import org.ethereum.beacon.ssz.visitor.SosSerializer.SerializerResult;

/** SSZ serializer/deserializer */
public class SSZSerializer implements BytesSerializer, SSZVisitorHandler<SerializerResult> {

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
    return visit(inputObject, inputClazz).getSerializedBody().getArrayUnsafe();
  }

  private <C> SerializerResult visit(C input, Class<? extends C> clazz) {
    return visitAny(typeResolver.resolveSSZType(new SSZField(clazz)), input);
  }

  @Override
  public SerializerResult visitAny(SSZType sszType, Object value) {
    return sszVisitorHost.handleAny(sszType, value, new SosSerializer());
  }

  @Override
  public SerializerResult visitList(SSZListType descriptor, Object listValue, int startIdx, int len) {
    return sszVisitorHost.handleSubList(descriptor, listValue, startIdx, len, new SosSerializer());
  }

  /**
   * Restores data instance from serialization data using {@link CompositeObjCreator}
   *
   * @param data SSZ serialization byte[] data
   * @param clazz type class
   * @return deserialized instance of clazz or throws exception
   */
  public <C> C decode(byte[] data, Class<? extends C> clazz) {
    Object decodeResult = sszVisitorHost.handleAny(
        typeResolver.resolveSSZType(new SSZField(clazz)),
        Bytes.wrap(data),
        new SosDeserializer());
    return (C) decodeResult;
  }
}
