package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class ProposerSlashing {
  @SSZ private final ValidatorIndex proposerIndex;
  @SSZ private final ProposalSignedData proposalData1;
  @SSZ private final BLSSignature proposalSignature1;
  @SSZ private final ProposalSignedData proposalData2;
  @SSZ private final BLSSignature proposalSignature2;

  public ProposerSlashing(
      ValidatorIndex proposerIndex,
      ProposalSignedData proposalData1,
      BLSSignature proposalSignature1,
      ProposalSignedData proposalData2,
      BLSSignature proposalSignature2) {
    this.proposerIndex = proposerIndex;
    this.proposalData1 = proposalData1;
    this.proposalSignature1 = proposalSignature1;
    this.proposalData2 = proposalData2;
    this.proposalSignature2 = proposalSignature2;
  }

  public ValidatorIndex getProposerIndex() {
    return proposerIndex;
  }

  public ProposalSignedData getProposalData1() {
    return proposalData1;
  }

  public BLSSignature getProposalSignature1() {
    return proposalSignature1;
  }

  public ProposalSignedData getProposalData2() {
    return proposalData2;
  }

  public BLSSignature getProposalSignature2() {
    return proposalSignature2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProposerSlashing that = (ProposerSlashing) o;
    return Objects.equal(proposerIndex, that.proposerIndex)
        && Objects.equal(proposalData1, that.proposalData1)
        && Objects.equal(proposalSignature1, that.proposalSignature1)
        && Objects.equal(proposalData2, that.proposalData2)
        && Objects.equal(proposalSignature2, that.proposalSignature2);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable ChainSpec spec, @Nullable Time beaconStart) {
    return "ProposerSlashing["
        + "proposer: " + proposerIndex
        + ", data1: " + proposalData1.toString(spec, beaconStart)
        + ", data2: " + proposalData2.toString(spec, beaconStart)
        + ", sig1: " + proposalSignature1
        + ", sig2: " + proposalSignature2
        + "]";
  }
}
