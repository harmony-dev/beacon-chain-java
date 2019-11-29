package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable(serializeAs = UInt64.class)
public class CommitteeIndex extends UInt64 implements
    SafeComparable<CommitteeIndex> {

  public static CommitteeIndex of(int index) {
    return new CommitteeIndex(UInt64.valueOf(index));
  }

  public static CommitteeIndex of(long index) {
    return new CommitteeIndex(UInt64.valueOf(index));
  }

  public CommitteeIndex(long value) {
    super(value);
  }

  public CommitteeIndex(UInt64 uint) {
    super(uint);
  }
}
