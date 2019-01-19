package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

@SSZSerializable
public class ProposerSlashing {
  @SSZ private final UInt24 proposerIndex;
  @SSZ private final ProposalSignedData proposalData1;
  @SSZ private final Bytes96 proposalSignature1;
  @SSZ private final ProposalSignedData proposalData2;
  @SSZ private final Bytes96 proposalSignature2;

  public ProposerSlashing(
      UInt24 proposerIndex,
      ProposalSignedData proposalData1,
      Bytes96 proposalSignature1,
      ProposalSignedData proposalData2,
      Bytes96 proposalSignature2) {
    this.proposerIndex = proposerIndex;
    this.proposalData1 = proposalData1;
    this.proposalSignature1 = proposalSignature1;
    this.proposalData2 = proposalData2;
    this.proposalSignature2 = proposalSignature2;
  }

  public UInt24 getProposerIndex() {
    return proposerIndex;
  }

  public ProposalSignedData getProposalData1() {
    return proposalData1;
  }

  public Bytes96 getProposalSignature1() {
    return proposalSignature1;
  }

  public ProposalSignedData getProposalData2() {
    return proposalData2;
  }

  public Bytes96 getProposalSignature2() {
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
}
