package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;

/**
 * Describes the specific SSZ type.
 * See <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#typing">
 *   official SSZ docs</a> for details.
 */
public interface SSZType {

  /**
   * Constant size indicating variable size
   */
  int VARIABLE_SIZE = -1;

  /**
   * Indicates this is basic SSZ type.
   * If true can be safely cast to {@link SSZBasicType}
   */
  default boolean isBasicType() {
    return false;
  }

  /**
   * Indicates this is Container SSZ type.
   * If true can be safely cast to {@link SSZContainerType} or {@link SSZCompositeType}
   */
  default boolean isContainer() {
    return false;
  }

  /**
   * Indicates this is List SSZ type.
   * If true can be safely cast to {@link SSZListType} or {@link SSZCompositeType}
   */
  default boolean isList() {
    return false;
  }

  /**
   * Indicates this is fixed size type.
   */
  default boolean isFixedSize() {
    return getSize() != VARIABLE_SIZE;
  }

  /**
   * Indicates this is variable size type.
   */
  default boolean isVariableSize() {
    return !isFixedSize();
  }

  /**
   * Gets the size of this type or {@link #VARIABLE_SIZE} if the type {@link #isVariableSize()}
   */
  int getSize();

  /**
   * Returns the type descriptor which contains accessor specific type info
   * @see org.ethereum.beacon.ssz.access.AccessorResolver
   * @see org.ethereum.beacon.ssz.access.SSZBasicAccessor
   * @see org.ethereum.beacon.ssz.access.SSZListAccessor
   * @see org.ethereum.beacon.ssz.access.SSZContainerAccessor
   */
  SSZField getTypeDescriptor();

  default String toStringHelper() {
    String type;
    if (isBasicType()) {
      type = "Basic";
    } else if (isContainer()) {
      type = "Container";
    } else if (isList()) {
      if (isFixedSize()) {
        type = "Vector";
      } else {
        type = "List";
      }
    } else {
      type = "Unknown";
    }
    return "SSZType[" + type + ", size=" + getSize() + ", descr: " + getTypeDescriptor() + "]";
  }

  default String dumpHierarchy() {
    return dumpHierarchy("");
  }

  default String dumpHierarchy(String indent) {
    String ret = "";
    ret += indent +  toStringHelper() + "\n";
    if (isList()) {
      ret += ((SSZListType) this).getElementType().dumpHierarchy(indent + "  ");
    }
    if (isContainer()) {
      for (SSZType sszType : ((SSZContainerType) this).getChildTypes()) {
        ret += sszType.dumpHierarchy(indent + "  ");
      }
    }
    return ret;
  }
}
