package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import javax.annotation.Nullable;

import org.ethereum.beacon.core.operations.slashing.Proposal;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class ProposerSlashing {
  @SSZ private final ValidatorIndex proposerIndex;
  @SSZ private final Proposal proposal1;
  @SSZ private final Proposal proposal2;

  public ProposerSlashing(
      ValidatorIndex proposerIndex,
      Proposal proposal1,
      Proposal proposal2) {
    this.proposerIndex = proposerIndex;
    this.proposal1 = proposal1;
    this.proposal2 = proposal2;
  }

  public ValidatorIndex getProposerIndex() {
    return proposerIndex;
  }

  public Proposal getProposal1() {
    return proposal1;
  }

  public Proposal getProposal2() {
    return proposal2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProposerSlashing that = (ProposerSlashing) o;
    return Objects.equal(proposerIndex, that.proposerIndex)
        && Objects.equal(proposal1, that.proposal1)
        && Objects.equal(proposal2, that.proposal2);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable ChainSpec spec, @Nullable Time beaconStart) {
    return "ProposerSlashing["
        + "proposer: " + proposerIndex
        + ", data1: " + proposal1.toString(spec, beaconStart)
        + ", data2: " + proposal2.toString(spec, beaconStart)
        + "]";
  }
}
