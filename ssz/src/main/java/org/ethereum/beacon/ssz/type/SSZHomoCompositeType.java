package org.ethereum.beacon.ssz.type;

/**
 * Describes homogeneous composite type (with children of a single type) like
 * List or Vector
 */
public interface SSZHomoCompositeType extends SSZCompositeType {

  /**
   * Returns the {@link SSZType} of this composite elements
   */
  SSZType getElementType();
}
