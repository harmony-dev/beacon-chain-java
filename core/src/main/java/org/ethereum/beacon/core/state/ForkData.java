package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Specifies hard fork parameters.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#forkdata">ForkData
 *     in the spec</a>
 */
@SSZSerializable
public class ForkData {
  public static final ForkData EMPTY = new ForkData(UInt64.ZERO, UInt64.ZERO, UInt64.ZERO);

  /** Previous fork version. */
  @SSZ private final UInt64 preForkVersion;
  /** Post fork version. */
  @SSZ private final UInt64 postForkVersion;
  /** Fork slot number. */
  @SSZ private final UInt64 forkSlot;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForkData forkData = (ForkData) o;
    return Objects.equal(preForkVersion, forkData.preForkVersion)
        && Objects.equal(postForkVersion, forkData.postForkVersion)
        && Objects.equal(forkSlot, forkData.forkSlot);
  }
}
