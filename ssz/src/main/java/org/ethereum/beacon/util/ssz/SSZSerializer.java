package org.ethereum.beacon.util.ssz;

import javafx.util.Pair;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.util.ssz.annotation.SSZ;
import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.util.ssz.annotation.SSZTransient;
import org.ethereum.beacon.util.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.util.ssz.type.BytesPrimitive;
import org.ethereum.beacon.util.ssz.type.Container;
import org.ethereum.beacon.util.ssz.type.SSZEncoderDecoder;
import org.ethereum.beacon.util.ssz.type.StringPrimitive;
import org.ethereum.beacon.util.ssz.type.UIntPrimitive;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ethereum.beacon.util.ssz.SSZSchemeBuilder.SSZScheme;

/**
 * <p>SSZ serializer/deserializer with automatic model reading
 * using Java class with annotations as model description.</p>
 *
 * <p>{@link SSZAnnotationSchemeBuilder} is used to create SSZ model
 * from Java class with annotations</p>
 * <p>
 * Following annotations are used for this:
 * <ul>
 * <li>{@link SSZSerializable} - Class which stores SSZ serializable data should be
 * annotated with it, any type which is not java.lang.*</li>
 * <li>{@link SSZ} - any field with Java type couldn't be automatically mapped to SSZ type,
 * or with mapping that overrides standard, should be annotated with it. For standard
 * mappings check {@link SSZ#type()} Javadoc.</li>
 * <li>{@link SSZTransient} - Fields that should not be used in serialization
 * should be marked with such annotation</li>
 * </ul>
 * </p>
 *
 * <p>For serialization use {@link #encode(Object)}. SSZ serializer
 * uses getters for all non-transient fields to get current values.</p>
 * <p>For deserialization, to restore instance use {@link #decode(byte[], Class)}.
 * It will try to find constructor with all non-transient field types and the same order,
 * to restore object. If failed, it will try to create empty instance from no-fields
 * constructor and set each one by one by appropriate setter.
 * If at least one field is failed to be set, {@link RuntimeException} is thrown.</p>
 */
public class SSZSerializer {

  public static int LENGTH_PREFIX_BYTE_SIZE = Integer.SIZE / Byte.SIZE;
  static byte[] EMPTY_PREFIX = new byte[LENGTH_PREFIX_BYTE_SIZE];

  private SSZSchemeBuilder schemeBuilder;
  private Map<Class, SSZEncoderDecoder> registeredClassHandlers = new HashMap<>();

  public SSZSerializer(SSZSchemeBuilder schemeBuilder) {
    this.schemeBuilder = schemeBuilder;
    new UIntPrimitive().registerIn(this);
    new BytesPrimitive().registerIn(this);
    new BooleanPrimitive().registerIn(this);
    new StringPrimitive().registerIn(this);
    new Container().registerIn(this);
  }

  public SSZSerializer withSSZEncoderDecoder(SSZEncoderDecoder encoderDecoder) {
    encoderDecoder.registerIn(this);
    return this;
  }

  /**
   * <p>Serializes input using SSZ serialization and annotations markup</p>
   * <p>Uses getters for all non-transient fields to get current values.</p>
   * @param input  input value
   * @param clazz  Class of value, should be marked {@link SSZSerializable}, for more
   *               information about annotation markup, check scheme builder
   *               {@link SSZAnnotationSchemeBuilder}
   * @return SSZ serialization
   */
  public byte[] encode(Object input, Class clazz) {
    checkSSZSerializableAnnotation(clazz);

    // Null check
    if (input == null) {
      return EMPTY_PREFIX;
    }

    // Fill up map with all available method getters
    Map<String, Method> getters = new HashMap<>();
    try {
      for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        getters.put(pd.getReadMethod().getName(), pd.getReadMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all getters in class %s",
          clazz.getName());
      throw new RuntimeException(error, e);
    }

    // Encode object fields one by one
    SSZScheme scheme = buildScheme(clazz);
    ByteArrayOutputStream res = new ByteArrayOutputStream();
    for (SSZScheme.SSZField field : scheme.fields) {
      Object value;
      Method getter = getters.get(field.getter);
      try {
        if (getter != null) {   // We have getter
          value = getter.invoke(input);
        } else {                // Trying to access field directly
          value = clazz.getField(field.name).get(input);
        }
      } catch (Exception e) {
        String error = String.format("Failed to get value from field %s, your should "
            + "either have public field or public getter for it", field.name);
        throw new RuntimeException(error);
      }

      SSZEncoderDecoder encoder = resolveEncoderDecoder(field);
      if (field.multipleType.equals(SSZScheme.MultipleType.NONE)) {
        encoder.encode(value, field, res);
      } else if (field.multipleType.equals(SSZScheme.MultipleType.LIST))  {
        encoder.encodeList((List<Object>) value, field, res);
      } else if (field.multipleType.equals(SSZScheme.MultipleType.ARRAY))  {
        encoder.encodeArray((Object[]) value, field, res);
      }
    }

    return res.toByteArray();
  }

  /**
   * <p>Shortcut to {@link #encode(Object, Class)}. Resolves
   * class using input object. Not suitable for null values.</p>
   */
  public byte[] encode(Object input) {
    return encode(input, input.getClass());
  }

  private static void checkSSZSerializableAnnotation(Class clazz) {
    if (!clazz.isAnnotationPresent(SSZSerializable.class)) {
      String error = String.format("Class %s should be annotated with SSZSerializable!", clazz);
      throw new RuntimeException(error);
    }
  }

  /**
   * Builds class scheme using {@link SSZSchemeBuilder}
   * @param clazz type class
   * @return SSZ model scheme
   */
  private SSZScheme buildScheme(Class clazz) {
    return schemeBuilder.build(clazz);
  }

  /**
   * <p>Restores data instance from serialization data using model with annotations markup</p>
   * <p>It will try to find constructor with all non-transient field types and the same order,
   * to restore object. If failed, it will try to create empty instance from no-fields
   * constructor and set each one by one by appropriate setter.
   * If at least one field is failed to be set, {@link RuntimeException} is thrown.</p>
   * @param data     SSZ serialization data
   * @param clazz    type class, should be marked {@link SSZSerializable}, for more
   *                 information about annotation markup, check {@link SSZAnnotationSchemeBuilder}
   * @return deserialized instance of clazz or throws exception
   */
  public Object decode(byte[] data, Class clazz) {
    checkSSZSerializableAnnotation(clazz);

    // Fast null handling
    if (Arrays.equals(data, EMPTY_PREFIX)) {
      return null;
    }

    SSZScheme scheme = buildScheme(clazz);
    List<SSZScheme.SSZField> fields = scheme.fields;
    int size = fields.size();
    Class[] params = new Class[size];
    Object[] values = new Object[size];
    BytesSSZReaderProxy reader = new BytesSSZReaderProxy(Bytes.of(data));

    // For each field resolve its type and decode its value
    for (int i = 0; i < size; i++) {
      SSZScheme.SSZField field = fields.get(i);
      switch (field.multipleType) {
        case NONE: {
          params[i] = field.type;
          break;
        }
        case LIST: {
          params[i] = List.class;
          break;
        }
        case ARRAY: {
          params[i] = Array.newInstance(field.type, 0).getClass();
          break;
        }
      }
      SSZEncoderDecoder decoder = resolveEncoderDecoder(field);
      if (field.multipleType.equals(SSZScheme.MultipleType.NONE)) {
        values[i] = decoder.decode(field, reader);
      } else if (field.multipleType.equals(SSZScheme.MultipleType.LIST)) {
        values[i] = decoder.decodeList(field, reader);
      } else if (field.multipleType.equals(SSZScheme.MultipleType.ARRAY)) {
        Object[] uncastedResult = decoder.decodeArray(field, reader);
        values[i] = Array.newInstance(field.type, uncastedResult.length);
        System.arraycopy(uncastedResult, 0, ((Object[]) values[i]), 0, uncastedResult.length);
      }
    }

    // Construct clazz instance
    Object result;
    Pair<Boolean, Object> constructorAttempt = createInstanceWithConstructor(clazz, params, values);
    if (!constructorAttempt.getKey()) {
      Pair<Boolean, Object> setterAttempt = createInstanceWithSetters(clazz, fields, values);
      if (!setterAttempt.getKey()) {
        String fieldTypes = Arrays.stream(values)
            .map(v -> v.getClass().toString())
            .collect(Collectors.joining(","));
        String error = String.format("Unable to find appropriate class %s "
            + "construction method with params [%s]."
            + "You should either have constructor with all non-transient fields "
            + "or setters/public fields.", clazz.getName(), fieldTypes);
        throw new RuntimeException(error);
      } else {
        result = setterAttempt.getValue();
      }
    } else {
      result = constructorAttempt.getValue();
    }

    return result;
  }

  private SSZEncoderDecoder resolveEncoderDecoder(SSZScheme.SSZField field) {
    SSZEncoderDecoder decoder;
    if (registeredClassHandlers.containsKey(field.type)) {
      decoder = registeredClassHandlers.get(field.type);
    } else {
      decoder = registeredClassHandlers.get(Object.class); // Container
    }

    return decoder;
  }

  private static Pair<Boolean, Object> createInstanceWithConstructor(Class clazz, Class[] params,
                                                                     Object[] values) {
    // Find constructor for params
    Constructor constructor;
    try {
      constructor = clazz.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return new Pair<>(false, null);
    }

    // Invoke constructor using values as params
    Object result;
    try {
      result = constructor.newInstance(values);
    } catch (Exception e) {
      return new Pair<>(false, null);
    }

    return new Pair<>(true, result);
  }

  private static Pair<Boolean, Object> createInstanceWithSetters(
      Class clazz, List<SSZScheme.SSZField> fields, Object[] values) {
    // Find constructor with no params
    Constructor constructor;
    try {
      constructor = clazz.getConstructor();
    } catch (NoSuchMethodException e) {
      return new Pair<>(false, null);
    }

    // Create empty instance
    Object result;
    try {
      result = constructor.newInstance();
    } catch (Exception e) {
      return new Pair<>(false, null);
    }

    Map<String, Method> fieldSetters = new HashMap<>();
    try {
      for (PropertyDescriptor pd: Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        fieldSetters.put(pd.getName(), pd.getWriteMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all setters in class %s", clazz.getName());
      throw new RuntimeException(error, e);
    }

    // Fill up field by field
    for (int i = 0; i < fields.size(); ++i) {
      SSZScheme.SSZField currentField = fields.get(i);
      try {   // Try to set by field assignment
        clazz.getField(currentField.name).set(result, values[i]);
      } catch (Exception e) {
        try {    // Try to set using setter
          fieldSetters.get(currentField.name).invoke(result, values[i]);
        } catch (Exception ex) {    // Cannot set the field
          return new Pair<>(false, null);
        }
      }
    }

    return new Pair<>(true, result);
  }

  public void registerClassTypes(Set<Class> classTypes, SSZEncoderDecoder typeHandler) {
    for (Class clazz : classTypes) {
      if (registeredClassHandlers.put(clazz, typeHandler) != null) {
        String error = String.format("Failed to register type %s handler, "
            + "this type already has its handler", clazz);
        throw new RuntimeException(error);
      };
    }
  }

  public void registerTypes(Set<String> types, SSZEncoderDecoder typeHandler) {
    // TODO: Do wee need this?
  }
}
