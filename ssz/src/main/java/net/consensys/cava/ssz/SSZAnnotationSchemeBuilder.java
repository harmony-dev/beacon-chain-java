package net.consensys.cava.ssz;

import net.consensys.cava.ssz.annotation.SSZ;
import net.consensys.cava.ssz.annotation.SSZSerializable;
import net.consensys.cava.ssz.annotation.SSZTransient;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static net.consensys.cava.ssz.SSZSerializer.extractType;

/**
 * <p>Builds scheme of SSZ serializable model
 * using Java class with annotations markup</p>
 * <p>Scheme builder could be initialized only once with one class,
 * create new instance for each new type</p>
 *
 * <p>
 * Following annotations are used for this:
 * <ul>
 * <li>{@link SSZSerializable} - Class which stores SSZ serializable data should be
 * annotated with it, any type which is not java.lang.*</li>
 * <li>{@link net.consensys.cava.ssz.annotation.SSZ} - any field with Java type
 * couldn't be automatically mapped to SSZ type,
 * or with mapping that overrides standard, should be annotated with it. For standard
 * mappings check {@link SSZ#type()} Javadoc.</li>
 * <li>{@link SSZTransient} - Fields that should not be used in serialization
 * should be marked with such annotation</li>
 * </ul>
 * </p>
 */
public class SSZAnnotationSchemeBuilder implements SSZSchemeBuilder {

  private Class clazz;
  private SSZScheme sszScheme = null;

  /**
   * <p>Initializes scheme builder with annotated class</p>
   * @param clazz Java class with SSZ annotations markup
   */
  public SSZAnnotationSchemeBuilder(Class clazz) {
    this.clazz = clazz;
  }

  /**
   * <p>Builds scheme and returns result.</p>
   * <p>Scheme is cached and could be returned again
   * without additional computation.</p>
   * @return scheme of SSZ model
   */
  @Override
  public SSZScheme build() {
    if (sszScheme != null) {
      return sszScheme;
    }

    SSZScheme scheme = new SSZScheme();
    SSZSerializable mainAnnotation = (SSZSerializable) clazz.getAnnotation(SSZSerializable.class);

    // Encode parameter means we don't need to serialize class
    // using any built-in logic, we should only call "encode"
    // method to get all object data and that's all!
    if(!mainAnnotation.encode().isEmpty()) {
      SSZScheme.SSZField encode = new SSZScheme.SSZField();
      encode.type = byte[].class;
      encode.sszType = SSZSerializer.SSZType.of(SSZSerializer.SSZType.Type.BYTES);
      encode.name = "encode";
      encode.getter = mainAnnotation.encode();
      scheme.fields.add(encode);
      return scheme;
    }

    // No encode parameter, build scheme field by field
    Map<String, Method> fieldGetters = new HashMap<>();
    try {
      for (PropertyDescriptor pd: Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        fieldGetters.put(pd.getName(), pd.getReadMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all getters in class %s", clazz.getName());
      throw new RuntimeException(error, e);
    }

    for (Field field : clazz.getDeclaredFields()) {

      // Skip SSZTransient
      boolean transientField = false;
      for (Annotation annotation : field.getAnnotations()) {
        if (annotation.annotationType().equals(SSZTransient.class)) {
          transientField = true;
          break;
        }
      }
      if (transientField) {
        continue;
      }

      // Check for SSZ annotation and read its parameters
      Class type = field.getType();
      net.consensys.cava.ssz.annotation.SSZ annotation = null;
      if (field.isAnnotationPresent(net.consensys.cava.ssz.annotation.SSZ.class)) {
        annotation = field.getAnnotation(net.consensys.cava.ssz.annotation.SSZ.class);
      }
      String typeAnnotation = null;
      if (annotation != null && !annotation.type().isEmpty()) {
        typeAnnotation = annotation.type();
      }

      // Construct SSZField
      SSZScheme.SSZField newField = new SSZScheme.SSZField();
      if (annotation != null && annotation.skipContainer()) {
        newField.skipContainer = true;
      }
      newField.type = type;
      String name = field.getName();
      newField.name = name;
      SSZSerializer.SSZType sszType = extractType(type, typeAnnotation);
      newField.sszType = sszType;
      if (sszType.type.equals(SSZSerializer.SSZType.Type.CONTAINER) && newField.skipContainer == null) {
        newField.skipContainer = false;
      }

      newField.getter = fieldGetters.containsKey(name) ? fieldGetters.get(name).getName() : null;
      scheme.fields.add(newField);
    }

    this.sszScheme = scheme;
    return scheme;
  }
}
