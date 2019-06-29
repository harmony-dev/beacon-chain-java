package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;

/**
 * Represent specific SSZ Basic type which can be either <code>uint8, uint16, ..., uint256</code>
 * or <code>bool</code> according to the
 * <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#basic-types">
 *   SSZ spec</a>
 */
public class SSZBasicType implements SSZType {

  private final SSZField descriptor;
  private final SSZBasicAccessor codec;
  private int size = Integer.MIN_VALUE;

  public SSZBasicType(SSZField descriptor, SSZBasicAccessor codec) {
    this.descriptor = descriptor;
    this.codec = codec;
  }

  @Override
  public Type getType() {
    return Type.BASIC;
  }

  /**
   * Returns the accessor which is capable of accessing corresponding Java type and
   * serializing/deserializing the value
   */
  public SSZBasicAccessor getAccessor() {
    return codec;
  }

  @Override
  public int getSize() {
    if (size == Integer.MIN_VALUE) {
      size = getAccessor().getSize(getTypeDescriptor());
    }
    return size;
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }
}
