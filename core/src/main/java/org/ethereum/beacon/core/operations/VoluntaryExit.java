package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
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

  /**
   * Minimum slot for processing exit.
   */
  @SSZ
  private final EpochNumber epoch;
  /**
   * Index of the exiting validator.
   */
  @SSZ
  private final ValidatorIndex validatorIndex;
  /**
   * Validator signature.
   */
  @SSZ
  private final BLSSignature signature;

  public VoluntaryExit(EpochNumber epoch, ValidatorIndex validatorIndex, BLSSignature signature) {
    this.epoch = epoch;
    this.validatorIndex = validatorIndex;
    this.signature = signature;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  public ValidatorIndex getValidatorIndex() {
    return validatorIndex;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    VoluntaryExit voluntaryExit = (VoluntaryExit) o;
    return Objects.equal(epoch, voluntaryExit.epoch)
        && Objects.equal(validatorIndex, voluntaryExit.validatorIndex)
        && Objects.equal(signature, voluntaryExit.signature);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(@Nullable SpecConstants spec) {
    return "VoluntaryExit["
        + "epoch=" + epoch.toString(spec)
        + ", validator=" + validatorIndex
        + ", sig=" + signature
        +"]";
  }
}