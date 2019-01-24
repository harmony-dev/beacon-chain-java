package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Keeps votes for a certain eth1 data.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#eth1datavote">Eth1DataVote</a>
 *     in the spec.
 */
@SSZSerializable
public class Eth1DataVote {

  /** Data being voted for. */
  @SSZ private final Eth1Data eth1Data;
  /** Vote count. */
  @SSZ private final UInt64 voteCount;

  public Eth1DataVote(Eth1Data eth1Data, UInt64 voteCount) {
    this.eth1Data = eth1Data;
    this.voteCount = voteCount;
  }

  public Eth1Data getEth1Data() {
    return eth1Data;
  }

  public UInt64 getVoteCount() {
    return voteCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Eth1DataVote that = (Eth1DataVote) o;
    return Objects.equal(eth1Data, that.eth1Data) && Objects.equal(voteCount, that.voteCount);
  }
}
