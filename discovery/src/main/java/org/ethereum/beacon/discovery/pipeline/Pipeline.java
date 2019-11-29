package org.ethereum.beacon.discovery.pipeline;

import org.reactivestreams.Publisher;

/**
 * Pipeline uses several {@link EnvelopeHandler} handlers to pass objects through the chain of
 * linked handlers implementing pipeline (or chain of responsibility) pattern.
 */
public interface Pipeline {
  /** Builds configured pipeline making it active */
  Pipeline build();

  /** Pushes object inside pipeline */
  void push(Object object);

  /** Adds handler at the end of current chain */
  Pipeline addHandler(EnvelopeHandler envelopeHandler);

  /** Stream from the exit of built pipeline */
  Publisher<Envelope> getOutgoingEnvelopes();
}
