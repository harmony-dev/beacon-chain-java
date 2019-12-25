package org.ethereum.beacon.core.envelops;

import com.google.common.base.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class SignedBeaconBlock {
  @SSZ private final BeaconBlock message;
  @SSZ private final BLSSignature signature;

  public SignedBeaconBlock(BeaconBlock message, BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public SignedBeaconBlock(SignedBeaconBlockHeader signedHeader, BeaconBlockBody body) {
    this.message = new BeaconBlock(signedHeader.getMessage(), body);
    this.signature = signedHeader.getSignature();
  }

  public BeaconBlock getMessage() {
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
    SignedBeaconBlock that = (SignedBeaconBlock) o;
    return Objects.equal(message, that.message) && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(message, signature);
  }

  @Override
  public String toString() {
    return toString(null, null, null);
  }

  public String toStringFull(@Nullable SpecConstants constants, @Nullable Time beaconStart,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    StringBuilder ret = new StringBuilder("Block["
        + toStringPriv(constants, beaconStart, hasher)
        + "]:\n");
    for (Attestation attestation : message.getBody().getAttestations()) {
      ret.append("  " + attestation.toString(constants, beaconStart) + "\n");
    }
    for (Deposit deposit : message.getBody().getDeposits()) {
      ret.append("  " + deposit.toString() + "\n");
    }
    for (SignedVoluntaryExit voluntaryExit : message.getBody().getVoluntaryExits()) {
      ret.append("  " + voluntaryExit.toString(constants) + "\n");
    }
    for (ProposerSlashing proposerSlashing : message.getBody().getProposerSlashings()) {
      ret.append("  " + proposerSlashing.toString(constants, hasher) + "\n");
    }

    for (AttesterSlashing attesterSlashing : message.getBody().getAttesterSlashings()) {
      ret.append("  " + attesterSlashing.toString() + "\n");
    }

    return ret.toString();
  }

  public String toString(@Nullable SpecConstants constants, @Nullable Time beaconStart,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    String ret = "Block[" + toStringPriv(constants, beaconStart, hasher);
    if (!message.getBody().getAttestations().isEmpty()) {
      ret += ", atts: [" + message.getBody().getAttestations().stream()
          .map(a -> a.toStringShort(constants))
          .collect(Collectors.joining(", ")) + "]";
    }
    if (!message.getBody().getDeposits().isEmpty()) {
      ret += ", depos: [" + message.getBody().getDeposits().stream()
          .map(Deposit::toString)
          .collect(Collectors.joining(", ")) + "]";
    }
    if (!message.getBody().getVoluntaryExits().isEmpty()) {
      ret += ", exits: [" + message.getBody().getVoluntaryExits().stream()
          .map(a -> a.toString(constants))
          .collect(Collectors.joining(", ")) + "]";
    }
    if (!message.getBody().getAttesterSlashings().isEmpty()) {
      ret += ", attSlash: [" + message.getBody().getAttesterSlashings().stream()
          .map(AttesterSlashing::toString)
          .collect(Collectors.joining(", ")) + "]";
    }
    if (!message.getBody().getProposerSlashings().isEmpty()) {
      ret += ", propSlash: [" + message.getBody().getProposerSlashings().stream()
          .map(a -> a.toString(constants, hasher))
          .collect(Collectors.joining(", ")) + "]";
    }
    ret += "]";

    return ret;
  }

  private String toStringPriv(@Nullable SpecConstants constants, @Nullable Time beaconStart,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return (hasher == null ? "?" : hasher.apply(this.message).toStringShort())
        + " <~ " + message.getParentRoot().toStringShort()
        + ", @slot " + message.getSlot().toStringNumber(constants)
        + ", state=" + message.getStateRoot().toStringShort()
        + ", randao=" + message.getBody().getRandaoReveal().toString()
        + ", " + message.getBody().getEth1Data()
        + ", sig=" + signature;
  }
}
