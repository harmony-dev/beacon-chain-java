package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes96;
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
  EpochNumber FAR_FUTURE_EPOCH = EpochNumber.castFrom(UInt64.MAX_VALUE); // (1 << 64) - 1
  Hash32 ZERO_HASH = Hash32.ZERO;
  Bytes1 BLS_WITHDRAWAL_PREFIX_BYTE = Bytes1.ZERO;

  /* Values defined in the spec. */

  default SlotNumber getGenesisSlot() {
    return GENESIS_SLOT;
  }

  default EpochNumber getGenesisEpoch() {
    return GENESIS_EPOCH;
  }

  default EpochNumber getFarFutureEpoch() {
    return FAR_FUTURE_EPOCH;
  }

  default Hash32 getZeroHash() {
    return ZERO_HASH;
  }

  default Bytes1 getBlsWithdrawalPrefixByte() {
    return BLS_WITHDRAWAL_PREFIX_BYTE;
  }
}
