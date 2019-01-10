package org.ethereum.beacon.core.spec;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Quotients that are used in reward and penalties calculation.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#reward-and-penalty-quotients">Reward
 *     and penalty quotients</a> in the spec.
 */
public interface RewardAndPenaltyQuotients {

  UInt64 getBaseRewardQuotient();

  UInt64 getWhistleblowerRewardQuotient();

  UInt64 getIncluderRewardQuotient();

  UInt64 getInactivityPenaltyQuotient();

  /* Values defined in the spec. */

  UInt64 BASE_REWARD_QUOTIENT = UInt64.valueOf(1 << 10); // 1024

  UInt64 WHISTLEBLOWER_REWARD_QUOTIENT = UInt64.valueOf(1 << 9); // 512

  UInt64 INCLUDER_REWARD_QUOTIENT = UInt64.valueOf(1 << 3); // 8

  UInt64 INACTIVITY_PENALTY_QUOTIENT = UInt64.valueOf(1 << 24); // 16_777_216
}
