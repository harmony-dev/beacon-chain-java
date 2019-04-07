package org.ethereum.beacon.ssz.access;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.ethereum.beacon.ssz.annotation.SSZ;

public class SSZField {
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
}
