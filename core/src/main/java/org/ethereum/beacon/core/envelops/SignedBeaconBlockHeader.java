package org.ethereum.beacon.core.envelops;

import com.google.common.base.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class SignedBeaconBlockHeader {
  @SSZ private final BeaconBlockHeader message;
  @SSZ private final BLSSignature signature;

  public SignedBeaconBlockHeader(BeaconBlockHeader message, BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public BeaconBlockHeader getMessage() {
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
    SignedBeaconBlockHeader that = (SignedBeaconBlockHeader) o;
    return Objects.equal(message, that.message) && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(message, signature);
  }

  public String toStringFull(
      @Nullable SpecConstants constants,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return "BlockHeader[" + toStringPriv(constants, hasher) + "]:\n";
  }

  public String toString(@Nullable SpecConstants constants,
      @Nullable Function<? super BeaconBlockHeader, Hash32> hasher) {
    return (hasher == null ? "?" : hasher.apply(message).toStringShort())
        + " <~ " + message.getParentRoot().toStringShort();
  }

  private String toStringPriv(@Nullable SpecConstants constants,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return (hasher == null ? "?" : hasher.apply(message).toStringShort())
        + " <~ " + message.getParentRoot().toStringShort()
        + ", @slot " + message.getSlot().toStringNumber(constants)
        + ", state=" + message.getStateRoot().toStringShort()
        + ", body=" + message.getBodyRoot().toStringShort()
        + ", sig=" + signature;
  }
}
