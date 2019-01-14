package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
/**
 * Specifies hard fork parameters.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#forkdata">ForkData
 *     in the spec</a>
 */
public class ForkData {
  public static final ForkData EMPTY = new ForkData(UInt64.ZERO, UInt64.ZERO, UInt64.ZERO);

  /** Previous fork version. */
  private final UInt64 preForkVersion;
  /** Post fork version. */
  private final UInt64 postForkVersion;
  /** Fork slot number. */
  private final UInt64 forkSlot;

  public ForkData(UInt64 preForkVersion, UInt64 postForkVersion, UInt64 forkSlot) {
    this.preForkVersion = preForkVersion;
    this.postForkVersion = postForkVersion;
    this.forkSlot = forkSlot;
  }

  public UInt64 getPreForkVersion() {
    return preForkVersion;
  }

  public UInt64 getPostForkVersion() {
    return postForkVersion;
  }

  public UInt64 getForkSlot() {
    return forkSlot;
  }
}
