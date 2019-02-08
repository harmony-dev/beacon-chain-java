package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Requests a quit from validator registry.
 *
 * @see BeaconBlockBody
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#exit">Exit
 *     in the spec</a>
 */
@SSZSerializable
public class Exit {

  /** Minimum slot for processing exit. */
  @SSZ private final EpochNumber epoch;
  /** Index of the exiting validator. */
  @SSZ private final ValidatorIndex validatorIndex;
  /** Validator signature. */
  @SSZ private final BLSSignature signature;

  public Exit(EpochNumber epoch, ValidatorIndex validatorIndex, BLSSignature signature) {
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Exit exit = (Exit) o;
    return Objects.equal(epoch, exit.epoch)
        && Objects.equal(validatorIndex, exit.validatorIndex)
        && Objects.equal(signature, exit.signature);
  }
}
