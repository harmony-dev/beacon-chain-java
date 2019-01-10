package org.ethereum.beacon.core.state;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Validator status flags.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#status-flags">Status
 *     flags</a> in the spec.
 */
public enum ValidatorStatusFlag {
  INITIATED_EXIT(UInt64.valueOf(1)),
  WITHDRAWABLE(UInt64.valueOf(2));

  private UInt64 value;

  ValidatorStatusFlag(UInt64 value) {
    this.value = value;
  }

  public UInt64 getValue() {
    return value;
  }
}
