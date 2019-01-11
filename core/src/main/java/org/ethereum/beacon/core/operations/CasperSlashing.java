package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class CasperSlashing {
  private final SlashableVoteData slashableVoteData1;
  private final SlashableVoteData slashableVoteData2;

  public CasperSlashing(
      SlashableVoteData slashableVoteData1, SlashableVoteData slashableVoteData2) {
    this.slashableVoteData1 = slashableVoteData1;
    this.slashableVoteData2 = slashableVoteData2;
  }

  public SlashableVoteData getSlashableVoteData1() {
    return slashableVoteData1;
  }

  public SlashableVoteData getSlashableVoteData2() {
    return slashableVoteData2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CasperSlashing that = (CasperSlashing) o;
    return slashableVoteData1.equals(that.slashableVoteData1) &&
        slashableVoteData2.equals(that.slashableVoteData2);
  }
}
