package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;

/**
 * State list lengths.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.1/specs/core/0_beacon-chain.md#state-list-lengths">State
 *     list lengths</a> in the spec.
 */
public interface StateListLengths {

  SlotNumber LATEST_BLOCK_ROOTS_LENGTH = SlotNumber.of(1 << 13); // 8192 block roots
  EpochNumber LATEST_RANDAO_MIXES_LENGTH = EpochNumber.of(1 << 13); // 8192 randao mixes
  EpochNumber LATEST_ACTIVE_INDEX_ROOTS_LENGTH = EpochNumber.of(1 << 13);
  EpochNumber LATEST_SLASHED_EXIT_LENGTH = EpochNumber.of(1 << 13); // 8192 epochs

  default SlotNumber getLatestBlockRootsLength() {
    return LATEST_BLOCK_ROOTS_LENGTH;
  }

  default EpochNumber getLatestRandaoMixesLength() {
    return LATEST_RANDAO_MIXES_LENGTH;
  }

  default EpochNumber getLatestActiveIndexRootsLength() {
    return LATEST_ACTIVE_INDEX_ROOTS_LENGTH;
  }

  default EpochNumber getSlashedExitLength() {
    return LATEST_SLASHED_EXIT_LENGTH;
  }
}
