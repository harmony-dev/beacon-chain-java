package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.SlotNumber.EpochLength;
import org.ethereum.beacon.core.types.Time;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Time parameters of the beacon chain.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#time-parameters">Time
 *     parameters</a> in the spec.
 */
public interface TimeParameters {

  Time SLOT_DURATION = Time.of(6); // 6 seconds
  SlotNumber MIN_ATTESTATION_INCLUSION_DELAY = SlotNumber.of(1 << 2); // 4 slots
  EpochLength EPOCH_LENGTH = new EpochLength(UInt64.valueOf(1 << 6)); // 64 slots
  SlotNumber  SEED_LOOKAHEAD = SlotNumber.of(1 << 6); // 64 slots
  SlotNumber ENTRY_EXIT_DELAY = SlotNumber.of(1 << 8); // 256 slots
  SlotNumber ETH1_DATA_VOTING_PERIOD = SlotNumber.of(1 << 10); // 1024 slots
  EpochNumber MIN_VALIDATOR_WITHDRAWAL_EPOCHS = EpochNumber.of(1 << 8);

  /* Values defined in the spec. */

  default Time getSlotDuration() {
    return SLOT_DURATION;
  }

  default SlotNumber getMinAttestationInclusionDelay() {
    return MIN_ATTESTATION_INCLUSION_DELAY;
  }

  default EpochLength getEpochLength() {
    return EPOCH_LENGTH;
  }

  default SlotNumber getSeedLookahead() {
    return SEED_LOOKAHEAD;
  }

  default SlotNumber getEntryExitDelay() {
    return ENTRY_EXIT_DELAY;
  }

  default SlotNumber getEth1DataVotingPeriod() {
    return ETH1_DATA_VOTING_PERIOD;
  }

  default EpochNumber getMinValidatorWithdrawalEpochs() {
    return MIN_VALIDATOR_WITHDRAWAL_EPOCHS;
  }
}
