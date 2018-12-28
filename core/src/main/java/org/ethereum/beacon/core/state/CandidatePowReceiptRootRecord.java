package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Keeps votes for a certain PoW receipt root.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#candidatepowreceiptrootrecord">CandidatePowReceiptRootRecord
 *     in the spec</a>
 */
public class CandidatePowReceiptRootRecord {

  /** Candidate PoW receipt root. */
  private final Hash32 candidatePowReceiptRoot;
  /** Vote count. */
  private final UInt64 voteCount;

  public CandidatePowReceiptRootRecord(Hash32 candidatePowReceiptRoot, UInt64 voteCount) {
    this.candidatePowReceiptRoot = candidatePowReceiptRoot;
    this.voteCount = voteCount;
  }

  public Hash32 getCandidatePowReceiptRoot() {
    return candidatePowReceiptRoot;
  }

  public UInt64 getVoteCount() {
    return voteCount;
  }
}
