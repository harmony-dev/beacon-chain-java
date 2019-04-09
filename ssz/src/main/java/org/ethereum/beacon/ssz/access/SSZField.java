package org.ethereum.beacon.ssz.access;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;

public class SSZField {

  public static SSZField resolveFromValue(Object value, Class<?> clazz) {
    if (value instanceof List && !((List) value).isEmpty()) {
      return new SSZField(
          new ParametrizedTypeImpl(clazz, ((List) value).get(0).getClass()));
    }
    return new SSZField(clazz);
  }

  public static SSZField resolveFromValue(Object value) {
    return resolveFromValue(value, value.getClass());
  }

  private final Type fieldType;
  private final SSZ fieldAnnotation;
  private final String extraType;
  private final Integer extraSize;
  private final String name;
  private final String getter;


  public SSZField(Type fieldType, SSZ fieldAnnotation, String extraType, Integer extraSize,
      String name, String getter) {
    this.fieldType = fieldType;
    this.fieldAnnotation = fieldAnnotation;
    this.extraType = extraType;
    this.extraSize = extraSize;
    this.name = name;
    this.getter = getter;
  }

  public SSZField(Type fieldType) {
    this(fieldType, null ,null, null, null, null);
    assert fieldType instanceof Class || fieldType instanceof ParameterizedType;
  }

  public Class<?> getRawClass() {
    return (Class<?>)
        (fieldType instanceof Class ? fieldType : ((ParameterizedType) fieldType).getRawType());
  }

  public ParameterizedType getParametrizedType() {
    return fieldType instanceof ParameterizedType ? (ParameterizedType) fieldType : null;
  }

  public SSZ getFieldAnnotation() {
    return fieldAnnotation;
  }

  public String getExtraType() {
    return extraType;
  }

  public Integer getExtraSize() {
    return extraSize;
  }

  public String getName() {
    return name;
  }

  public String getGetter() {
    return getter;
  }

  @Override
  public String toString() {
    return "SSZField{" +
        "fieldClass=" + fieldType +
        ", extraType='" + extraType + '\'' +
        ", extraSize=" + extraSize +
        ", name='" + name + '\'' +
        ", getter='" + getter + '\'' +
        '}';
  }

  private static class ParametrizedTypeImpl implements ParameterizedType {
    private final Type rawType;
    private final Type[] actualTypeArguments;

    public ParametrizedTypeImpl(Type rawType, Type... actualTypeArguments) {
      this.rawType = rawType;
      this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public Type getOwnerType() {
      return null;
    }
  }
}
