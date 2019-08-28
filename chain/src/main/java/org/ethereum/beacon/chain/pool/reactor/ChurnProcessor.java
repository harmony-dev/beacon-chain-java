package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

public class ChurnProcessor
    extends AbstractDelegateProcessor<Object, OffChainAggregates> {

  @Override
  protected void hookOnNext(Object value) {
    // TODO implement
  }
}
