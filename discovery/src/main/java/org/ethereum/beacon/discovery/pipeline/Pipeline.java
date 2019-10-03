package org.ethereum.beacon.discovery.pipeline;

import org.reactivestreams.Publisher;

public interface Pipeline {
  Pipeline build();

  void send(Object object);

  Pipeline addHandler(EnvelopeHandler envelopeHandler);

  Publisher<Envelope> getOutgoingEnvelopes();
}
