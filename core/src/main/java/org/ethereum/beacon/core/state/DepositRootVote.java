package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Keeps votes for a certain PoW receipt root.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#depositrootvote">DepositRootVote
 *     in the spec</a>
 */
@SSZSerializable
public class DepositRootVote {

  /** Receipt root of registration contract on the PoW chain. */
  @SSZ private final Hash32 depositRoot;
  /** Vote count. */
  @SSZ private final UInt64 voteCount;

  public DepositRootVote(Hash32 depositRoot, UInt64 voteCount) {
    this.depositRoot = depositRoot;
    this.voteCount = voteCount;
  }

  public Hash32 getDepositRoot() {
    return depositRoot;
  }

  public UInt64 getVoteCount() {
    return voteCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DepositRootVote that = (DepositRootVote) o;
    return Objects.equal(depositRoot, that.depositRoot) && Objects.equal(voteCount, that.voteCount);
  }
}
