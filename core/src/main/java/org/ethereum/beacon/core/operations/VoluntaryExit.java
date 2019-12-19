package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * Requests a quit from validator registry.
 *
 * @see BeaconBlockBody
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#voluntaryexit">VoluntaryExit
 *     in the spec</a>
 */
@SSZSerializable
public class VoluntaryExit {

  /** Earliest epoch when voluntary exit can be processed. */
  @SSZ private final EpochNumber epoch;
  /** Index of the exiting validator. */
  @SSZ private final ValidatorIndex validatorIndex;

  public VoluntaryExit(EpochNumber epoch, ValidatorIndex validatorIndex) {
    this.epoch = epoch;
    this.validatorIndex = validatorIndex;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  public ValidatorIndex getValidatorIndex() {
    return validatorIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    VoluntaryExit voluntaryExit = (VoluntaryExit) o;
    return Objects.equal(epoch, voluntaryExit.epoch)
        && Objects.equal(validatorIndex, voluntaryExit.validatorIndex);
  }

  @Override
  public int hashCode() {
    int result = epoch.hashCode();
    result = 31 * result + validatorIndex.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(@Nullable SpecConstants spec) {
    return "VoluntaryExit["
        + "epoch=" + epoch.toString(spec)
        + ", validator=" + validatorIndex
        +"]";
  }
}