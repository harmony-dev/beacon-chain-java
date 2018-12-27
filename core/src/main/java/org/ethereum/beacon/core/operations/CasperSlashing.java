package org.ethereum.beacon.core.operations;

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
}
