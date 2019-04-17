package org.ethereum.beacon.ssz.access.container;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.ssz.access.SSZField;

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

  }
}
