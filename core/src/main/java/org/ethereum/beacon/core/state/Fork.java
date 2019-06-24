package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes4;

/**
 * Specifies hard fork parameters.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#fork">Fork
 *     in the spec</a>
 */
@SSZSerializable
public class Fork {
  public static final Fork EMPTY = new Fork(Bytes4.ZERO, Bytes4.ZERO, EpochNumber.of(0));

  /** Previous fork version. */
  @SSZ private final Bytes4 previousVersion;
  /** Post fork version. */
  @SSZ private final Bytes4 currentVersion;
  /** Fork slot number. */
  @SSZ private final EpochNumber epoch;

  public Fork(Bytes4 previousVersion, Bytes4 currentVersion,
      EpochNumber epoch) {
    this.previousVersion = previousVersion;
    this.currentVersion = currentVersion;
    this.epoch = epoch;
  }

  public Bytes4 getPreviousVersion() {
    return previousVersion;
  }

  public Bytes4 getCurrentVersion() {
    return currentVersion;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Fork fork = (Fork) o;
    return Objects.equal(previousVersion, fork.previousVersion)
        && Objects.equal(currentVersion, fork.currentVersion)
        && Objects.equal(epoch, fork.epoch);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(
      @Nullable SpecConstants spec) {
    return "Fork[" + epoch.toString(spec) + ", " + previousVersion + " => " + currentVersion + "]";
  }
}
