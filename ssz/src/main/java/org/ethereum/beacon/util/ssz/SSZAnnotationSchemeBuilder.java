package org.ethereum.beacon.util.ssz;

import javafx.util.Pair;
import org.ethereum.beacon.util.ssz.annotation.SSZ;
import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.util.ssz.annotation.SSZTransient;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <li>{@link SSZ} - any field with Java type
 * couldn't be automatically mapped to SSZ type,
 * or with mapping that overrides standard, should be annotated with it. For standard
 * mappings check {@link SSZ#type()} Javadoc.</li>
 * <li>{@link SSZTransient} - Fields that should not be used in serialization
 * should be marked with such annotation</li>
 * </ul>
 * </p>
 */
public class SSZAnnotationSchemeBuilder implements SSZSchemeBuilder {

  private static final String TYPE_REGEX = "^(\\D+)((\\d+)?)$";

  private Logger logger = null;

  private boolean explicitFieldAnnotation = true;

  public SSZAnnotationSchemeBuilder() {
  }

  /**
   * Whether to require {@link SSZ} annotation for field to be included,
   * non-transient or not.
   * Default: {@link SSZ} required for each field
   * When `explicitFieldAnnotation` set to false, all private fields are included,
   * unless marked with {@link SSZTransient}
   * @param explicitFieldAnnotation   Require {@link SSZ} annotation
   *                                  for field to be included in scheme
   */
  public SSZAnnotationSchemeBuilder(boolean explicitFieldAnnotation) {
    this.explicitFieldAnnotation = explicitFieldAnnotation;
  }

  public SSZAnnotationSchemeBuilder withLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  /**
   * <p>Builds scheme and returns result.</p>
   * <p>Scheme is cached and could be returned again
   * without additional computation.</p>
   * @return scheme of SSZ model
   */
  @Override
  public SSZScheme build(Class clazz) {
    SSZScheme scheme = new SSZScheme();
    SSZSerializable mainAnnotation = (SSZSerializable) clazz.getAnnotation(SSZSerializable.class);

    // Encode parameter means we don't need to serialize class
    // using any built-in logic, we should only call "encode"
    // method to get all object data and that's all!
    if(!mainAnnotation.encode().isEmpty()) {
      SSZScheme.SSZField encode = new SSZScheme.SSZField();
      encode.type = byte[].class;
      encode.extraType = "bytes";
      encode.name = "encode";
      encode.getter = mainAnnotation.encode();
      scheme.fields.add(encode);
      return logAndReturnScheme(clazz, scheme);
    }

    // No encode parameter, build scheme field by field
    Map<String, Method> fieldGetters = new HashMap<>();
    try {
      for (PropertyDescriptor pd: Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        fieldGetters.put(pd.getName(), pd.getReadMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all getters in class %s", clazz.getName());
      throw new SSZSchemeException(error, e);
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
      SSZ annotation = null;
      if (field.isAnnotationPresent(SSZ.class)) {
        annotation = field.getAnnotation(SSZ.class);
      } else {
        if (explicitFieldAnnotation) {  // Skip field if explicit annotation is required
          continue;
        }
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (isStatic) {  // Skip static fields if it's no marked by @SSZ annotation
          continue;
        }
      }
      String typeAnnotation = null;
      if (annotation != null && !annotation.type().isEmpty()) {
        typeAnnotation = annotation.type();
      }

      // Construct SSZField
      SSZScheme.SSZField newField = new SSZScheme.SSZField();
      if (annotation != null && annotation.skipContainer()) {
        newField.notAContainer = true;
      }
      newField.type = type;
      String name = field.getName();
      newField.name = name;
      if (typeAnnotation != null) {
        Pair<String, Integer> extra = extractType(typeAnnotation, type);
        newField.extraType = extra.getKey();
        newField.extraSize = extra.getValue();
      }
      if (type.equals(List.class)) {
        newField.multipleType = SSZScheme.MultipleType.LIST;
        newField.type = extractListInternalType(field);
      } else if (type.isArray() && !type.getComponentType().isPrimitive()) {
        newField.multipleType = SSZScheme.MultipleType.ARRAY;
        newField.type = type.getComponentType();
      }

      newField.getter = fieldGetters.containsKey(name) ? fieldGetters.get(name).getName() : null;
      scheme.fields.add(newField);
    }

    return logAndReturnScheme(clazz, scheme);
  }

  private SSZScheme logAndReturnScheme(Class clazz, SSZScheme scheme) {
    if (logger == null) {
      return scheme;
    }
    String overview = String.format("Scheme for class %s consists of %s field(s)",
        clazz.getName(), scheme.fields.size());
    logger.info(overview);
    for (SSZScheme.SSZField field : scheme.fields) {
      logger.info(field.toString());
    }

    return scheme;
  }

  private Class extractListInternalType(Field field) {
    Type genericFieldType = field.getGenericType();
    Class res = null;

    if(genericFieldType instanceof ParameterizedType){
      ParameterizedType aType = (ParameterizedType) genericFieldType;
      Type[] fieldArgTypes = aType.getActualTypeArguments();
      for(Type fieldArgType : fieldArgTypes){
        Class fieldArgClass = (Class) fieldArgType;
        if (res == null) {
          res = fieldArgClass;
        } else {
          String error = String.format("Could not extract list type from field %s", field.getName());
          throw new SSZSchemeException(error);
        }
      }
    }

    return res;
  }

  private static Pair<String, Integer> extractType(String extra, Class clazz) {
    String extraType;
    Integer extraSize = null;
    Pattern pattern = Pattern.compile(TYPE_REGEX);
    Matcher matcher = pattern.matcher(extra);
    if (matcher.find()) {
      String type = matcher.group(1);
      String endNumber = matcher.group(3);
      extraType = type;

      if (endNumber != null) {
        extraSize = Integer.valueOf(endNumber);
      }
    } else {
      String error = String.format("Type annotation \"%s\" for class %s is not correct",
          extra, clazz.getName());
      throw new SSZSchemeException(error);
    }

    return new Pair<>(extraType, extraSize);
  }
}
