package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Initial values.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#initial-values">Initial
 *     values</a> in the spec.
 */
public interface InitialValues {

  SlotNumber GENESIS_SLOT = SlotNumber.ZERO;
  EpochNumber GENESIS_EPOCH = EpochNumber.ZERO;
  UInt64 BLS_WITHDRAWAL_PREFIX = UInt64.ZERO;

  /* Values defined in the spec. */

  default SlotNumber getGenesisSlot() {
    return GENESIS_SLOT;
  }

  default EpochNumber getGenesisEpoch() {
    return GENESIS_EPOCH;
  }

  default UInt64 getBlsWithdrawalPrefix() {
    return BLS_WITHDRAWAL_PREFIX;
  }
}
