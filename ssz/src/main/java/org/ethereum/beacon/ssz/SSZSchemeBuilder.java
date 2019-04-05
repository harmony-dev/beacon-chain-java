package org.ethereum.beacon.ssz;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;

/** Builds SSZScheme using SSZ model info provided via constructor or predefined */
public interface SSZSchemeBuilder {

  SSZScheme build(Class clazz);

  /**
   * Object SSZ scheme.
   *
   * <p>Enumerates all object fields and their properties in appropriate order. Order matters!
   */
  class SSZScheme {
    private List<SSZField> fields = new ArrayList<>();

    public List<SSZField> getFields() {
      return fields;
    }

    public static class SSZField {
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
  }
}
