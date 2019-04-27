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

  UInt64 GENESIS_FORK_VERSION = UInt64.ZERO;
  SlotNumber GENESIS_SLOT = SlotNumber.ZERO;
  EpochNumber GENESIS_EPOCH = EpochNumber.ZERO;
  ShardNumber GENESIS_START_SHARD = ShardNumber.of(0);
  EpochNumber FAR_FUTURE_EPOCH = EpochNumber.castFrom(UInt64.MAX_VALUE); // (1 << 64) - 1
  Hash32 ZERO_HASH = Hash32.ZERO;
  BLSSignature EMPTY_SIGNATURE = BLSSignature.ZERO;
  Bytes1 BLS_WITHDRAWAL_PREFIX_BYTE = Bytes1.ZERO;

  /* Values defined in the spec. */

  default UInt64 getGenesisForkVersion() {
    return GENESIS_FORK_VERSION;
  }

  default SlotNumber getGenesisSlot() {
    return GENESIS_SLOT;
  }

  default EpochNumber getGenesisEpoch() {
    return GENESIS_EPOCH;
  }

  default ShardNumber getGenesisStartShard() {
    return GENESIS_START_SHARD;
  }

  default EpochNumber getFarFutureEpoch() {
    return FAR_FUTURE_EPOCH;
  }

  default Hash32 getZeroHash() {
    return ZERO_HASH;
  }

  default BLSSignature getEmptySignature() {
    return EMPTY_SIGNATURE;
  }

  default Bytes1 getBlsWithdrawalPrefixByte() {
    return BLS_WITHDRAWAL_PREFIX_BYTE;
  }
}
