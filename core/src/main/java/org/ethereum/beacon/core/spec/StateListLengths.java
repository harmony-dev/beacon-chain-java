package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * State list lengths.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.1/specs/core/0_beacon-chain.md#state-list-lengths">State
 *     list lengths</a> in the spec.
 */
public interface StateListLengths {

  EpochNumber EPOCHS_PER_HISTORICAL_VECTOR = EpochNumber.of(1 << 16); // 65,536 epochs
  EpochNumber EPOCHS_PER_SLASHINGS_VECTOR = EpochNumber.of(1 << 13); // 8,192 epochs
  UInt64 HISTORICAL_ROOTS_LIMIT = UInt64.valueOf(1 << 24); // 16,777,216
  UInt64 VALIDATOR_REGISTRY_LIMIT = UInt64.valueOf(1L << 40); // 1,099,511,627,776 validators

  default EpochNumber getEpochsPerHistoricalVector() {
    return EPOCHS_PER_HISTORICAL_VECTOR;
  }

  default EpochNumber getEpochsPerSlashingsVector() {
    return EPOCHS_PER_SLASHINGS_VECTOR;
  }

  default UInt64 getHistoricalRootsLimit() {
    return HISTORICAL_ROOTS_LIMIT;
  }

  default UInt64 getValidatorRegistryLimit() {
    return VALIDATOR_REGISTRY_LIMIT;
  }
}
