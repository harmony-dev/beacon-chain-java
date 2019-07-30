package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadVector;

/**
 * A batch of historical data.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.1/specs/core/0_beacon-chain.md#historicalbatch">HistoricalBatch</a>
 *     in the spec.
 */
@SSZSerializable
public class HistoricalBatch {

  /** Block roots. */
  @SSZ(vectorLengthVar = "spec.SLOTS_PER_HISTORICAL_ROOT")
  private final ReadVector<SlotNumber, Hash32> blockRoots;
  /** State roots. */
  @SSZ(vectorLengthVar = "spec.SLOTS_PER_HISTORICAL_ROOT")
  private final ReadVector<SlotNumber, Hash32> stateRoots;

  public HistoricalBatch(ReadVector<SlotNumber, Hash32> blockRoots, ReadVector<SlotNumber, Hash32> stateRoots) {
    this.blockRoots = blockRoots;
    this.stateRoots = stateRoots;
  }

  public ReadVector<SlotNumber, Hash32> getBlockRoots() {
    return blockRoots;
  }

  public ReadVector<SlotNumber, Hash32> getStateRoots() {
    return stateRoots;
  }
}
