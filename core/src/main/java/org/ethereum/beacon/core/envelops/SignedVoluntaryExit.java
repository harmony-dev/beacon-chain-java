package org.ethereum.beacon.core.envelops;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class SignedVoluntaryExit {
  @SSZ private final VoluntaryExit message;
  @SSZ private final BLSSignature signature;

  public SignedVoluntaryExit(VoluntaryExit message, BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public VoluntaryExit getMessage() {
    return message;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignedVoluntaryExit that = (SignedVoluntaryExit) o;
    return Objects.equal(message, that.message) && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(message, signature);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(@Nullable SpecConstants spec) {
    return "VoluntaryExit["
        + "epoch=" + message.getEpoch().toString(spec)
        + ", validator=" + message.getValidatorIndex()
        + ", sig=" + signature
        +"]";
  }
}
