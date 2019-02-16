package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.WrappingBytes96;

@SSZSerializable(serializeAs = Bytes96.class)
public class BLSSignature extends WrappingBytes96 {

  public static final BLSSignature ZERO = new BLSSignature(Bytes96.ZERO);

  public static BLSSignature wrap(Bytes96 signatureBytes) {
    return new BLSSignature(signatureBytes);
  }

  public BLSSignature(Bytes96 value) {
    super(value);
  }

  @Override
  public String toString() {
    String s = super.toString();
    return s.substring(2, 6) + "..." + s.substring(190);
  }
}
