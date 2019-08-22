package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;

public abstract class AbstractProcessor implements AttestationProcessor {
  protected final SimpleProcessor<ReceivedAttestation> outbound;

  public AbstractProcessor(Schedulers schedulers, String name) {
    this.outbound = new SimpleProcessor<>(schedulers.events(), name + ".outbound");
  }

  @Override
  public Publisher<ReceivedAttestation> out() {
    return outbound;
  }
}
