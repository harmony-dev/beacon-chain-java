package org.ethereum.beacon.ssz;

import java.util.ArrayList;
import java.util.List;

/** Builds SSZScheme using SSZ model info provided via constructor or predefined */
public interface SSZSchemeBuilder {

  SSZScheme build(Class clazz);

  /**
   * Object SSZ scheme.
   *
   * <p>Enumerates all object fields and their properties in appropriate order. Order matters!
   */
  class SSZScheme {
    List<SSZField> fields = new ArrayList<>();

    public enum MultipleType {
      NONE,
      LIST,
      ARRAY
    }

    public static class SSZField {
      public Class type;
      public MultipleType multipleType = MultipleType.NONE;
      public String extraType = null;
      public Integer extraSize = null;
      public String name;
      public String getter;
      /**
       * Special type that could look like a container by using type unhandled with
       * encoder/decoders, but holds only one standard SSZ value, which should be not wrapped with
       * extra header like container
       */
      public boolean notAContainer = false;

      @Override
      public String toString() {
        return "SSZField{"
            + "type="
            + type
            + ", multipleType="
            + multipleType
            + ", extraType='"
            + extraType
            + '\''
            + ", extraSize="
            + extraSize
            + ", name='"
            + name
            + '\''
            + ", getter='"
            + getter
            + '\''
            + ", notAContainer="
            + notAContainer
            + '}';
      }
    }
  }
}
