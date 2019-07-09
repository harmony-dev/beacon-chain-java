package org.ethereum.beacon.ssz.access;

import org.ethereum.beacon.ssz.access.SSZCompositeAccessor.CompositeInstanceAccessor;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Handles collections of homogeneous objects (e.g. java array, {@link java.util.List}, etc) which
 * includes both ssz types: vectors and lists List accessor is responsible of accessing list
 * elements, their type and new instance creation
 *
 * <p>This interface also serves as its own {@link CompositeInstanceAccessor} since there is
 * normally no information to cache about the list type (as opposed to {@link SSZContainerAccessor})
 */
public interface SSZListAccessor extends SSZCompositeAccessor, CompositeInstanceAccessor {

  @Override
  int getChildrenCount(Object value);

  /**
   * Returns atomic size of a list. Normally it's 1:1 to element size, see {@link
   * #getChildrenCount(Object)}, but assuming structures like Bit List we have elements that are
   * smaller than byte (atoms) which are accessible as bytes
   */
  default int getAtomicChildrenCount(Object value) {
    return getChildrenCount(value);
  }

  /**
   * Converts atomic size of a list to size of the list with minimal obtainable elements. Normally
   * it's 1:1, but assuming Bit List we have elements that are smaller than byte (atoms) which are
   * accessible as bytes
   */
  default int fromAtomicSize(int claimedSize) {
    return claimedSize;
  }

  @Override
  Object getChildValue(Object value, int idx);

  /**
   * Given the list type returns the type descriptor of its elements
   *
   * @param listTypeDescriptor
   * @return List elements type
   */
  SSZField getListElementType(SSZField listTypeDescriptor);

  @Override
  ListInstanceBuilder createInstanceBuilder(SSZType sszType);

  /**
   * Some types of lists have extra size information encoded during serialization, which should be
   * removed for hashing. Just use this method.
   */
  default BytesValue removeListSize(Object value, BytesValue serialization) {
    return serialization;
  }

  @Override
  default CompositeInstanceAccessor getInstanceAccessor(SSZField compositeDescriptor) {
    return this;
  }

  interface ListInstanceBuilder extends CompositeInstanceBuilder {

    /** Appends a new child to the building list */
    void addChild(Object childValue);
  }
}
