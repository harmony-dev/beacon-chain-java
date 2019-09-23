package org.ethereum.beacon.chain.pool;

import java.util.List;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A DTO for aggregated attestations that are not yet included on chain.
 *
 * <p>Beacon block proposer should be fed with this data.
 */
public class OffChainAggregates {
  private final Hash32 blockRoot;
  private final SlotNumber slot;
  private final List<AttestationAggregate> aggregates;

  public OffChainAggregates(
      Hash32 blockRoot, SlotNumber slot, List<AttestationAggregate> aggregates) {
    this.blockRoot = blockRoot;
    this.slot = slot;
    this.aggregates = aggregates;
  }

  public Hash32 getBlockRoot() {
    return blockRoot;
  }

  public List<AttestationAggregate> getAggregates() {
    return aggregates;
  }

  public SlotNumber getSlot() {
    return slot;
  }
}
