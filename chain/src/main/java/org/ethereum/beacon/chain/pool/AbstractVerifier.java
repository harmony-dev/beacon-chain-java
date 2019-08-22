package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;

public abstract class AbstractVerifier implements AttestationVerifier {
  protected final SimpleProcessor<ReceivedAttestation> valid;
  protected final SimpleProcessor<ReceivedAttestation> invalid;

  public AbstractVerifier(Schedulers schedulers, String name) {
    this.valid = new SimpleProcessor<>(schedulers.events(), name + ".valid");
    this.invalid = new SimpleProcessor<>(schedulers.events(), name + ".invalid");
  }

  @Override
  public Publisher<ReceivedAttestation> valid() {
    return valid;
  }

  @Override
  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }
}
