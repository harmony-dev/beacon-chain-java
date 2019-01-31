package org.ethereum.beacon.core.spec;

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
  UInt64 GENESIS_SLOT = UInt64.valueOf(1 << 19); // 524,288
  UInt64 GENESIS_START_SHARD = UInt64.ZERO;
  UInt64 FAR_FUTURE_SLOT = UInt64.MAX_VALUE; // (1 << 64) - 1
  Bytes96 EMPTY_SIGNATURE = Bytes96.ZERO;
  Bytes1 BLS_WITHDRAWAL_PREFIX_BYTE = Bytes1.ZERO;

  /* Values defined in the spec. */

  UInt64 getGenesisForkVersion();

  UInt64 getGenesisSlot();

  UInt64 getGenesisStartShard();

  UInt64 getFarFutureSlot();

  Bytes96 getEmptySignature();

  Bytes1 getBlsWithdrawalPrefixByte();
}
