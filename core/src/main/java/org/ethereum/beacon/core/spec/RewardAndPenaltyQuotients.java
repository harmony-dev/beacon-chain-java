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

  UInt64 BASE_REWARD_QUOTIENT = UInt64.valueOf(1 << 5); // 1024
  UInt64 WHISTLEBLOWER_REWARD_QUOTIENT = UInt64.valueOf(1 << 9); // 512
  UInt64 ATTESTATION_INCLUSION_REWARD_QUOTIENT = UInt64.valueOf(1 << 3); // 8
  UInt64 INACTIVITY_PENALTY_QUOTIENT = UInt64.valueOf(1 << 24); // 16_777_216
  UInt64 MIN_PENALTY_QUOTIENT = UInt64.valueOf(1 << 5); // 32

  /* Values defined in the spec. */

  default UInt64 getBaseRewardQuotient() {
    return BASE_REWARD_QUOTIENT;
  }

  default UInt64 getWhistleblowerRewardQuotient() {
    return WHISTLEBLOWER_REWARD_QUOTIENT;
  }

  default UInt64 getAttestationInclusionRewardQuotient() {
    return ATTESTATION_INCLUSION_REWARD_QUOTIENT;
  }

  default UInt64 getInactivityPenaltyQuotient() {
    return INACTIVITY_PENALTY_QUOTIENT;
  }

  default UInt64 getMinPenaltyQuotient() {
    return MIN_PENALTY_QUOTIENT;
  }
}
