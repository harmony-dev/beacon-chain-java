package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

public class AttestationChurnProcessor
    extends AbstractDelegateProcessor<Input, OffChainAggregates> {

  @Override
  protected void hookOnNext(Input value) {
    // TODO implement
  }
}
