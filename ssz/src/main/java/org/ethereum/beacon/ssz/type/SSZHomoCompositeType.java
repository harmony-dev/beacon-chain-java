package org.ethereum.beacon.ssz.type;

public interface SSZHomoCompositeType extends SSZCompositeType {

  /**
   * Returns the {@link SSZType} of this composite elements
   */
  SSZType getElementType();
}
