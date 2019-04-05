package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.access.SSZCodec;

public class SSZBasicType implements SSZType {

  private final SSZField descriptor;
  private final SSZCodec codec;

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
    return getValueCodec().getSize(getTypeDescriptor());
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }
}
