package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.WrappingBytes96;

public class BLSSignature extends WrappingBytes96 {

  public static final BLSSignature ZERO = new BLSSignature(Bytes96.ZERO);

  public static BLSSignature wrap(Bytes96 signatureBytes) {
    return new BLSSignature(signatureBytes);
  }

  private BLSSignature(Bytes96 value) {
    super(value);
  }
}
