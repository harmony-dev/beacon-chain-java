package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.network.NetworkParcelV5;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import reactor.core.publisher.FluxSink;

import static org.ethereum.beacon.discovery.pipeline.PipelineImpl.INCOMING;

public class OutgoingParcelHandler implements EnvelopeHandler {

  private final FluxSink<NetworkParcel> outgoingSink;

  public OutgoingParcelHandler(FluxSink<NetworkParcel> outgoingSink) {
    this.outgoingSink = outgoingSink;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(INCOMING)) {
      return;
    }
    if (envelope.get(INCOMING) instanceof NetworkParcel) {
      outgoingSink.next((NetworkParcel) envelope.get(INCOMING));
      envelope.remove(INCOMING);
    }
  }
}
