package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class ProposerSlashing {
  @SSZ private final ValidatorIndex proposerIndex;
  @SSZ private final BeaconBlockHeader header1;
  @SSZ private final BeaconBlockHeader header2;

  public ProposerSlashing(
      ValidatorIndex proposerIndex, BeaconBlockHeader header1, BeaconBlockHeader header2) {
    this.proposerIndex = proposerIndex;
    this.header1 = header1;
    this.header2 = header2;
  }

  public ValidatorIndex getProposerIndex() {
    return proposerIndex;
  }

  public BeaconBlockHeader getHeader1() {
    return header1;
  }

  public BeaconBlockHeader getHeader2() {
    return header2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProposerSlashing that = (ProposerSlashing) o;
    return Objects.equal(proposerIndex, that.proposerIndex)
        && Objects.equal(header1, that.header1)
        && Objects.equal(header2, that.header2);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return "ProposerSlashing["
        + "proposer: " + proposerIndex
        + ", header1: " + header1.toStringFull(spec, hasher)
        + ", header2: " + header1.toStringFull(spec, hasher)
        + "]";
  }
}
