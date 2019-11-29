package org.ethereum.beacon.discovery.pipeline;

public interface EnvelopeHandler {
  void handle(Envelope envelope);
}
