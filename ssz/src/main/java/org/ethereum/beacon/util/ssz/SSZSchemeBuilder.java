package org.ethereum.beacon.util.ssz;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds SSZScheme using SSZ model info
 * provided via constructor or predefined
 */
public interface SSZSchemeBuilder {

  SSZScheme build(Class clazz);

  /**
   * <p>Object SSZ scheme.</p>
   *
   * <p>Enumerates all object fields and their
   * properties in appropriate order. Order matters!</p>
   */
  class SSZScheme {
    List<SSZField> fields = new ArrayList<>();

    public static class SSZField {
      public Class type;
      public MultipleType multipleType = MultipleType.NONE;
      public String extraType = null;
      public Integer extraSize = null;
      public String name;
      public String getter;
      /**
       * <ul>
       * <li>Container not needed (primitive type) : null</li>
       * <li>Container needed : false</li>
       * <li>Container needed but should be omitted: true</li>
       * </ul>
       */
      public Boolean skipContainer = null;
    }

    public enum MultipleType {
      NONE, LIST, ARRAY
    }
  }
}
