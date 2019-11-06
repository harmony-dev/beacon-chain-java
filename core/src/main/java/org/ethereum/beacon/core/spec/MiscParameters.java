package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.Time;
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
  UInt64 MAX_COMMITTEES_PER_SLOT = UInt64.valueOf(1 << 6); // 64
  ValidatorIndex TARGET_COMMITTEE_SIZE = ValidatorIndex.of(1 << 7); // 128 validators
  UInt64 MAX_VALIDATORS_PER_COMMITTEE = UInt64.valueOf(1 << 11); // 2048
  UInt64 MIN_PER_EPOCH_CHURN_LIMIT = UInt64.valueOf(1 << 2); // 4
  UInt64 CHURN_LIMIT_QUOTIENT = UInt64.valueOf(1 << 16); // 65_536
  int SHUFFLE_ROUND_COUNT = 90;
  UInt64 MIN_GENESIS_ACTIVE_VALIDATOR_COUNT = UInt64.valueOf(1 << 16); // 65_536
  Time MIN_GENESIS_TIME = Time.of(1578009600L);

  /* Values defined in the spec. */

  default ShardNumber getShardCount() {
    return SHARD_COUNT;
  }

  default UInt64 getMaxCommitteesPerSlot() {
    return MAX_COMMITTEES_PER_SLOT;
  }

  default ValidatorIndex getTargetCommitteeSize() {
    return TARGET_COMMITTEE_SIZE;
  }

  default UInt64 getMaxValidatorsPerCommittee() {
    return MAX_VALIDATORS_PER_COMMITTEE;
  }

  default UInt64 getMinPerEpochChurnLimit() {
    return MIN_PER_EPOCH_CHURN_LIMIT;
  }

  default UInt64 getChurnLimitQuotient() {
    return CHURN_LIMIT_QUOTIENT;
  }

  default int getShuffleRoundCount() {
    return SHUFFLE_ROUND_COUNT;
  }

  default UInt64 getMinGenesisActiveValidatorCount() {
    return MIN_GENESIS_ACTIVE_VALIDATOR_COUNT;
  }

  default Time getMinGenesisTime() {
    return MIN_GENESIS_TIME;
  }
}
