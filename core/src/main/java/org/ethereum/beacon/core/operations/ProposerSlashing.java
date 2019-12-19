package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class ProposerSlashing {
  @SSZ private final ValidatorIndex proposerIndex;
  @SSZ private final SignedBeaconBlockHeader signedHeader1;
  @SSZ private final SignedBeaconBlockHeader signedHeader2;

  public ProposerSlashing(
      ValidatorIndex proposerIndex, SignedBeaconBlockHeader signedHeader1, SignedBeaconBlockHeader signedHeader2) {
    this.proposerIndex = proposerIndex;
    this.signedHeader1 = signedHeader1;
    this.signedHeader2 = signedHeader2;
  }

  public ValidatorIndex getProposerIndex() {
    return proposerIndex;
  }

  public SignedBeaconBlockHeader getSignedHeader1() {
    return signedHeader1;
  }

  public SignedBeaconBlockHeader getSignedHeader2() {
    return signedHeader2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProposerSlashing that = (ProposerSlashing) o;
    return Objects.equal(proposerIndex, that.proposerIndex)
        && Objects.equal(signedHeader1, that.signedHeader1)
        && Objects.equal(signedHeader2, that.signedHeader2);
  }

  @Override
  public int hashCode() {
    int result = proposerIndex.hashCode();
    result = 31 * result + signedHeader1.hashCode();
    result = 31 * result + signedHeader2.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return "ProposerSlashing["
        + "proposer: " + proposerIndex
        + ", header1: " + signedHeader1.toStringFull(spec, hasher)
        + ", header2: " + signedHeader1.toStringFull(spec, hasher)
        + "]";
  }
}
