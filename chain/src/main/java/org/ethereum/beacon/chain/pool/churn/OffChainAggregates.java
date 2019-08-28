package org.ethereum.beacon.chain.pool.churn;

import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A DTO for aggregated attestations that are not yet included on chain.
 *
 * <p>Beacon block proposer should be fed with this data.
 */
public class OffChainAggregates {
  private final Hash32 blockRoot;
  private final List<Attestation> aggregates;

  public OffChainAggregates(Hash32 blockRoot, List<Attestation> aggregates) {
    this.blockRoot = blockRoot;
    this.aggregates = aggregates;
  }

  public Hash32 getBlockRoot() {
    return blockRoot;
  }

  public List<Attestation> getAggregates() {
    return aggregates;
  }
}
