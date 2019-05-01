package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Misc beacon chain constants.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#misc">Misc</a>
 *     in the spec.
 */
public interface MiscParameters {

  ShardNumber SHARD_COUNT = ShardNumber.of(1 << 10); // 1024 shards
  ValidatorIndex TARGET_COMMITTEE_SIZE = ValidatorIndex.of(1 << 7); // 128 validators
  UInt64 MAX_BALANCE_CHURN_QUOTIENT = UInt64.valueOf(1 << 5); // 32
  ShardNumber BEACON_CHAIN_SHARD_NUMBER = ShardNumber.of(UInt64.MAX_VALUE); // (1 << 64) - 1
  UInt64 MAX_INDICES_PER_ATTESTATION = UInt64.valueOf(1 << 12); // 4096
  UInt64 MIN_PER_EPOCH_CHURN_LIMIT = UInt64.valueOf(1 << 2); // 4
  UInt64 CHURN_LIMIT_QUOTIENT = UInt64.valueOf(1 << 16); // 65_536
  UInt64 BASE_REWARDS_PER_EPOCH = UInt64.valueOf(5);
  UInt64 MAX_EXIT_DEQUEUES_PER_EPOCH = UInt64.valueOf(1 << 2); // 4
  int SHUFFLE_ROUND_COUNT = 90;

  /* Values defined in the spec. */

  default ShardNumber getShardCount() {
    return SHARD_COUNT;
  }

  default ValidatorIndex getTargetCommitteeSize() {
    return TARGET_COMMITTEE_SIZE;
  }

  default UInt64 getMaxBalanceChurnQuotient() {
    return MAX_BALANCE_CHURN_QUOTIENT;
  }

  default ShardNumber getBeaconChainShardNumber() {
    return BEACON_CHAIN_SHARD_NUMBER;
  }

  default UInt64 getMaxIndicesPerAttestation() {
    return MAX_INDICES_PER_ATTESTATION;
  }

  default UInt64 getMinPerEpochChurnLimit() {
    return MIN_PER_EPOCH_CHURN_LIMIT;
  }

  default UInt64 getChurnLimitQuotient() {
    return CHURN_LIMIT_QUOTIENT;
  }

  default UInt64 getBaseRewardsPerEpoch() {
    return BASE_REWARDS_PER_EPOCH;
  }

  default UInt64 getMaxExitDequesPerEpoch() {
    return MAX_EXIT_DEQUEUES_PER_EPOCH;
  }

  default int getShuffleRoundCount() {
    return SHUFFLE_ROUND_COUNT;
  }
}
