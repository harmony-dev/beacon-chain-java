package org.ethereum.beacon.core.spec;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Time parameters of the beacon chain.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#time-parameters">Time
 *     parameters</a> in the spec.
 */
public interface TimeParameters {

  UInt64 getSlotDuration();

  UInt64 getMinAttestationInclusionDelay();

  UInt64 getEpochLength();

  UInt64 getSeedLookahead();

  UInt64 getEntryExitDelay();

  UInt64 getDepositRootVotingPeriod();

  UInt64 getMinValidatorWithdrawalTime();

  /* Values defined in the spec. */

  UInt64 SLOT_DURATION = UInt64.valueOf(6); // 6 seconds

  UInt64 MIN_ATTESTATION_INCLUSION_DELAY = UInt64.valueOf(1 << 2); // 4 slots

  UInt64 EPOCH_LENGTH = UInt64.valueOf(1 << 6); // 64 slots

  UInt64 SEED_LOOKAHEAD = UInt64.valueOf(1 << 6); // 64 slots

  UInt64 ENTRY_EXIT_DELAY = UInt64.valueOf(1 << 8); // 256 slots

  UInt64 DEPOSIT_ROOT_VOTING_PERIOD = UInt64.valueOf(1 << 10); // 1024 slots

  UInt64 MIN_VALIDATOR_WITHDRAWAL_TIME = UInt64.valueOf(1 << 14); // 16384 slots
}
