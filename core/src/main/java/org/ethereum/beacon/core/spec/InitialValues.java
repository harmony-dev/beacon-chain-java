package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
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
  SlotNumber GENESIS_SLOT = SlotNumber.of(1 << 19); // 2**19
  ShardNumber GENESIS_START_SHARD = ShardNumber.of(0);
  SlotNumber FAR_FUTURE_SLOT = SlotNumber.castFrom(UInt64.MAX_VALUE); // (1 << 64) - 1
  BLSSignature EMPTY_SIGNATURE = BLSSignature.ZERO;
  Bytes1 BLS_WITHDRAWAL_PREFIX_BYTE = Bytes1.ZERO;

  /* Values defined in the spec. */

  UInt64 getGenesisForkVersion();

  SlotNumber getGenesisSlot();

  ShardNumber getGenesisStartShard();

  SlotNumber getFarFutureSlot();

  BLSSignature getEmptySignature();

  Bytes1 getBlsWithdrawalPrefixByte();
}
