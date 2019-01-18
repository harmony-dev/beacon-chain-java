package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * Challenges to slash validator violated Casper slashing conditions.
 *
 * @see BeaconBlockBody
 * @see SlashableVoteData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#casperslashing">CasperSlashing
 *     in the spec</a>
 */
@SSZSerializable
public class CasperSlashing {

  /** First batch of votes. */
  @SSZ private final SlashableVoteData slashableVoteData1;
  /** Second batch of votes. */
  @SSZ private final SlashableVoteData slashableVoteData2;

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
    return Objects.equal(slashableVoteData1, that.slashableVoteData1)
        && Objects.equal(slashableVoteData2, that.slashableVoteData2);
  }
}
