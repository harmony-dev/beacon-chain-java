package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZ;

/**
 * {@link tech.pegasys.artemis.util.collections.Bitlist} and {@link
 * tech.pegasys.artemis.util.collections.Bitvector}
 *
 * <p>It's like a list/vector, but we have elements inside that are smaller than our usual elements:
 * bits instead of bytes. So we mimic old interfaces in bytes and add some new interfaces in bits
 */
public class SSZBitListType extends SSZListType {

  public SSZBitListType(
      SSZField descriptor,
      TypeResolver typeResolver,
      SSZListAccessor accessor,
      int vectorLength,
      long maxSize) {
    super(descriptor, typeResolver, accessor, vectorLength, maxSize);
  }

  /**
   * If this type represents SSZ Vector then this method returns its length.
   *
   * @see SSZ#vectorLength()
   * @see SSZ#vectorLengthVar()
   */
  @Override
  public int getVectorLength() {
    return (int) fromAtomicSize(super.getVectorLength());
  }

  public int getBitSize() {
    return super.getVectorLength();
  }

  private long fromAtomicSize(long atomicSize) {
    int addon = getType() == Type.LIST ? 8 : 7;
    return atomicSize == SSZType.VARIABLE_SIZE ? atomicSize : (atomicSize + addon) / 8;
  }

  @Override
  public long getMaxSize() {
    return fromAtomicSize(super.getMaxSize());
  }

  public long getMaxBitSize() {
    return super.getMaxSize();
  }
}
