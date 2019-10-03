package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.ethereum.beacon.discovery.pipeline.PipelineImpl.INCOMING;

public class IncomingDataHandler implements EnvelopeHandler {

  public static final String UNKNOWN = "unknown";

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(INCOMING)) {
      return;
    }
    envelope.put(UNKNOWN, new UnknownPacket((BytesValue) envelope.get(INCOMING)));
    envelope.remove(INCOMING);
  }
}
