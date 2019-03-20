package org.ethereum.beacon.core.state;

import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A batch of historical data.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#historicalbatch">HistoricalBatch</a>
 *     in the spec.
 */
@SSZSerializable
public class HistoricalBatch {

  /** Block roots. */
  @SSZ private final List<Hash32> blockRoots;
  /** State roots. */
  @SSZ private final List<Hash32> stateRoots;

  public HistoricalBatch(List<Hash32> blockRoots, List<Hash32> stateRoots) {
    this.blockRoots = blockRoots;
    this.stateRoots = stateRoots;
  }

  public List<Hash32> getBlockRoots() {
    return blockRoots;
  }

  public List<Hash32> getStateRoots() {
    return stateRoots;
  }
}
