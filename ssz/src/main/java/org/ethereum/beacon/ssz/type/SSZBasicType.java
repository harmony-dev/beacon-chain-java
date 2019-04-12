package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZCodec;

public class SSZBasicType implements SSZType {

  private final SSZField descriptor;
  private final SSZCodec codec;
  private int size = Integer.MIN_VALUE;

  public SSZBasicType(SSZField descriptor, SSZCodec codec) {
    this.descriptor = descriptor;
    this.codec = codec;
  }

  @Override
  public boolean isBasicType() {
    return true;
  }

  public SSZCodec getValueCodec() {
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
