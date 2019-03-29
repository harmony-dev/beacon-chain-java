package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import org.ethereum.beacon.ssz.visitor.SSZVisitor;
import org.ethereum.beacon.ssz.visitor.SSZCompositeType;
import org.ethereum.beacon.ssz.visitor.SSZCompositeValue;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHall;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHandler;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import javax.annotation.Nullable;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme;

/** SSZ serializer/deserializer */
public class SSZSerializer implements BytesSerializer, SSZVisitorHandler<SSZSimpleSerializer.SSZSerializerResult> {

  public static final int LENGTH_PREFIX_BYTE_SIZE = Integer.SIZE / Byte.SIZE;
  static final byte[] EMPTY_PREFIX = new byte[LENGTH_PREFIX_BYTE_SIZE];

  private SSZSchemeBuilder schemeBuilder;

  private SSZCodecResolver codecResolver;

  private SSZModelFactory sszModelFactory;

  private final SSZVisitorHall sszVisitorHall;
  private final SSZSimpleSerializer simpleSerializer;

  /**
   * SSZ serializer/deserializer with following helpers
   *
   * @param schemeBuilder SSZ model scheme building of type {@link SSZScheme}
   * @param codecResolver Resolves field encoder/decoder {@link
   *     org.ethereum.beacon.ssz.type.SSZCodec} function
   * @param sszModelFactory Instantiates SSZModel with field/data information
   */
  public SSZSerializer(
      SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver,
      SSZModelFactory sszModelFactory) {
    this.schemeBuilder = schemeBuilder;
    this.codecResolver = codecResolver;
    this.sszModelFactory = sszModelFactory;
    sszVisitorHall = new SSZVisitorHall(schemeBuilder, codecResolver);
    simpleSerializer = new SSZSimpleSerializer(codecResolver);
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
  public SSZSerializerResult visitAny(SSZField descriptor, Object value) {
    return sszVisitorHall.handleAny(descriptor, value, simpleSerializer);
  }

  @Override
  public <C> SSZSerializerResult visit(C input, Class<? extends C> clazz) {
    return visitAny(new SSZField(clazz), input);
  }

  /**
   * Builds class scheme using {@link SSZSchemeBuilder}
   *
   * @param clazz type class
   * @return SSZ model scheme
   */
  private SSZScheme buildScheme(Class clazz) {
    return schemeBuilder.build(clazz);
  }

  /**
   * Restores data instance from serialization data using {@link SSZModelFactory}
   *
   * @param data SSZ serialization byte[] data
   * @param clazz type class
   * @return deserialized instance of clazz or throws exception
   */
  @Override
  public <C> C decode(byte[] data, Class<? extends C> clazz) {
    checkSSZSerializableAnnotation(clazz);

    SSZScheme scheme = buildScheme(clazz);
    List<SSZScheme.SSZField> fields = scheme.getFields();
    int size = fields.size();
    BytesSSZReaderProxy reader = new BytesSSZReaderProxy(Bytes.of(data));
    List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs = new ArrayList<>();

    // For each field resolve its type and decode its value
    for (int i = 0; i < size; i++) {
      SSZScheme.SSZField field = fields.get(i);
      Object obj = codecResolver.resolveDecodeFunction(field).apply(new Pair<>(reader, this));
      fieldValuePairs.add(new Pair<>(field, obj));
    }

    if (!reader.isComplete()) {
      throw new RuntimeException(
          String.format(
              "Provided data is not valid for object of type %s, "
                  + "data is not over but all fields are read!",
              clazz));
    }

    return sszModelFactory.create(clazz, fieldValuePairs);
  }
}
