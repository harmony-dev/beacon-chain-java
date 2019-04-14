package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;

public class SSZBasicType implements SSZType {

  private final SSZField descriptor;
  private final SSZBasicAccessor codec;
  private int size = Integer.MIN_VALUE;

  public SSZBasicType(SSZField descriptor, SSZBasicAccessor codec) {
    this.descriptor = descriptor;
    this.codec = codec;
  }

  @Override
  public boolean isBasicType() {
    return true;
  }

  public SSZBasicAccessor getValueCodec() {
    return codec;
  }

  @Override
  public int getSize() {
    if (size == Integer.MIN_VALUE) {
      size = getValueCodec().getSize(getTypeDescriptor());
    }
    return size;
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }
}
