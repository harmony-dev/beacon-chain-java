package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZCompositeAccessor;
import org.ethereum.beacon.ssz.type.list.SSZListType;

/**
 * Common superinterface for {@link SSZListType} and {@link SSZContainerType} types
 * which both have children elements accessible by their index
 */
public interface SSZCompositeType extends SSZType {

  /**
   * Shortcut for
   * <code>this.getAccessor().getAccessor(this.getTypeDescriptor()).getChildrenCount(value)</code>
   * Returns the children count of the composite value Java representation instance
   */
  default int getChildrenCount(Object value) {
    return getAccessor().getInstanceAccessor(getTypeDescriptor()).getChildrenCount(value);
  }

  /**
   * Shortcut for
   * <code>this.getAccessor().getAccessor(this.getTypeDescriptor()).getChildValue(value, idx)</code>
   * Returns the child at index of the composite value Java representation instance
   */
  default Object getChild(Object value, int idx) {
    return getAccessor().getInstanceAccessor(getTypeDescriptor()).getChildValue(value, idx);
  }

  /**
   * Returns the corresponding {@link SSZCompositeAccessor} instance
   */
  SSZCompositeAccessor getAccessor();
}
