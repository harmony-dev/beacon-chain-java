package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
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
public class SSZSerializer implements BytesSerializer {

  public static final int LENGTH_PREFIX_BYTE_SIZE = Integer.SIZE / Byte.SIZE;
  static final byte[] EMPTY_PREFIX = new byte[LENGTH_PREFIX_BYTE_SIZE];

  private SSZSchemeBuilder schemeBuilder;

  private SSZCodecResolver codecResolver;

  private SSZModelFactory sszModelFactory;

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
    checkSSZSerializableAnnotation(inputClazz);

    // Null check
    if (inputObject == null) {
      return EMPTY_PREFIX;
    }

    Object input;
    Class<?> clazz;
    if (!inputClazz.getAnnotation(SSZSerializable.class).instanceGetter().isEmpty()) {
      try {
        Method instanceGetter = inputClazz
            .getMethod(inputClazz.getAnnotation(SSZSerializable.class).instanceGetter());
        input = instanceGetter.invoke(inputObject);
        clazz = input.getClass();
      } catch (Exception e) {
        throw new RuntimeException("Error processing SSZSerializable.instanceGetter attribute", e);
      }
    } else {
      input = inputObject;
      clazz = inputClazz;
    }

    // Fill up map with all available method getters
    Map<String, Method> getters = new HashMap<>();
    try {
      for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        getters.put(pd.getReadMethod().getName(), pd.getReadMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all getters in class %s", clazz.getName());
      throw new RuntimeException(error, e);
    }

    // Encode object fields one by one
    SSZScheme scheme = buildScheme(clazz);
    ByteArrayOutputStream res = new ByteArrayOutputStream();
    for (SSZScheme.SSZField field : scheme.getFields()) {
      Object value;
      Method getter = getters.get(field.getter);
      try {
        if (getter != null) { // We have getter
          value = getter.invoke(input);
        } else { // Trying to access field directly
          value = clazz.getField(field.name).get(input);
        }
      } catch (Exception e) {
        String error =
            String.format(
                "Failed to get value from field %s, your should "
                    + "either have public field or public getter for it",
                field.name);
        throw new SSZSchemeException(error);
      }

      codecResolver.resolveEncodeFunction(field).accept(new Triplet<>(value, res, this));
    }

    return res.toByteArray();
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
