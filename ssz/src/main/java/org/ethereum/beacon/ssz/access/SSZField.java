package org.ethereum.beacon.ssz.access;

import java.lang.reflect.ParameterizedType;
import org.ethereum.beacon.ssz.annotation.SSZ;

public class SSZField {
  public Class<?> fieldType;
  public ParameterizedType fieldGenericType = null;
  public SSZ fieldAnnotation;
  public String extraType = null;
  public Integer extraSize = null;
  public String name;
  public String getter;

  public SSZField() {
  }

  public SSZField(Class<?> fieldType) {
    this.fieldType = fieldType;
  }

  @Override
  public String toString() {
    return "SSZField{" +
        "fieldClass=" + fieldType +
        ", fieldGenericType=" + fieldGenericType +
        ", extraType='" + extraType + '\'' +
        ", extraSize=" + extraSize +
        ", name='" + name + '\'' +
        ", getter='" + getter + '\'' +
        '}';
  }
}
