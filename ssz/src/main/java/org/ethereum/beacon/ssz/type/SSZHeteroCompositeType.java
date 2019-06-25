package org.ethereum.beacon.ssz.type;

import java.util.List;

public interface SSZHeteroCompositeType extends SSZCompositeType {

  /**
   * Returns a list of this Container/Union children types
   */
  List<SSZType> getChildTypes();
}
