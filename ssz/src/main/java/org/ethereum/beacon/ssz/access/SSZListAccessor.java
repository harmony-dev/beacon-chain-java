package org.ethereum.beacon.ssz.access;

import org.ethereum.beacon.ssz.access.SSZCompositeAccessor.CompositeInstanceAccessor;

/**
 * Handles collections of homogeneous objects (e.g. java array, {@link java.util.List}, etc)
 * which includes both ssz types: vectors and lists
 * List accessor is responsible of accessing list elements, their type and new instance creation
 *
 * This interface also serves as its own {@link CompositeInstanceAccessor}
 * since there is normally no information to cache about the list type
 * (as opposed to {@link SSZContainerAccessor})
 */
public interface SSZListAccessor extends SSZCompositeAccessor, CompositeInstanceAccessor {

  interface ListInstanceBuilder extends CompositeInstanceBuilder {

    /**
     * Appends a new child to the building list
     */
    void addChild(Object childValue);
  }

  @Override
  int getChildrenCount(Object value);

  @Override
  Object getChildValue(Object value, int idx);

  /**
   * Given the list type returns the type descriptor of its elements
   * @param listTypeDescriptor
   * @return List elements type
   */
  SSZField getListElementType(SSZField listTypeDescriptor);

  @Override
  ListInstanceBuilder createInstanceBuilder(SSZField listType);

  @Override
  default CompositeInstanceAccessor getInstanceAccessor(SSZField compositeDescriptor) {
    return this;
  }
}
