package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.SlotNumber.EpochLength;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Time parameters of the beacon chain.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#time-parameters">Time
 *     parameters</a> in the spec.
 */
public interface TimeParameters {

  UInt64 SLOT_DURATION = UInt64.valueOf(6); // 6 seconds
  SlotNumber MIN_ATTESTATION_INCLUSION_DELAY = SlotNumber.of(1 << 2); // 4 slots
  EpochLength EPOCH_LENGTH = new EpochLength(UInt64.valueOf(1 << 6)); // 64 slots
  SlotNumber  SEED_LOOKAHEAD = SlotNumber.of(1 << 6); // 64 slots
  SlotNumber ENTRY_EXIT_DELAY = SlotNumber.of(1 << 8); // 256 slots
  SlotNumber ETH1_DATA_VOTING_PERIOD = SlotNumber.of(1 << 10); // 1024 slots
  SlotNumber MIN_VALIDATOR_WITHDRAWAL_TIME = SlotNumber.of(1 << 14); // 16384 slots

  /* Values defined in the spec. */

  UInt64 getSlotDuration();

  SlotNumber getMinAttestationInclusionDelay();

  EpochLength getEpochLength();

  SlotNumber getSeedLookahead();

  SlotNumber getEntryExitDelay();

  SlotNumber getEth1DataVotingPeriod();

  SlotNumber getMinValidatorWithdrawalTime();
}
