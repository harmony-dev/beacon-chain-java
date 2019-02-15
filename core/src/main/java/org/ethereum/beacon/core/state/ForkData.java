package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
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
  public static final ForkData EMPTY = new ForkData(UInt64.ZERO, UInt64.ZERO, EpochNumber.of(0));

  /** Previous fork version. */
  @SSZ private final UInt64 previousVersion;
  /** Post fork version. */
  @SSZ private final UInt64 currentVersion;
  /** Fork slot number. */
  @SSZ private final EpochNumber epoch;

  public ForkData(UInt64 previousVersion, UInt64 currentVersion,
      EpochNumber epoch) {
    this.previousVersion = previousVersion;
    this.currentVersion = currentVersion;
    this.epoch = epoch;
  }

  public UInt64 getPreviousVersion() {
    return previousVersion;
  }

  public UInt64 getCurrentVersion() {
    return currentVersion;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForkData forkData = (ForkData) o;
    return Objects.equal(previousVersion, forkData.previousVersion)
        && Objects.equal(currentVersion, forkData.currentVersion)
        && Objects.equal(epoch, forkData.epoch);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(
      @Nullable ChainSpec spec) {
    return "Fork[" + epoch.toString(spec) + ", " + previousVersion + " => " + currentVersion + "]";
  }
}
