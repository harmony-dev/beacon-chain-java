package org.ethereum.beacon.ssz.type;

import java.util.List;

/**
 * Describes heterogeneous composite type (with children of different types) like
 * Container or Union
 */
public interface SSZHeteroCompositeType extends SSZCompositeType {

  /**
   * Returns a list of this Container/Union children types
   */
  List<SSZType> getChildTypes();
}
