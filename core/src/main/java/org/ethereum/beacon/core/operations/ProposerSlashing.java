package org.ethereum.beacon.core.operations;

import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

public class ProposerSlashing {
  private final UInt24 proposerIndex;
  private final ProposalSignedData proposalData1;
  private final Bytes96 proposalSignature1;
  private final ProposalSignedData proposalData2;
  private final Bytes96 proposalSignature2;

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
}
