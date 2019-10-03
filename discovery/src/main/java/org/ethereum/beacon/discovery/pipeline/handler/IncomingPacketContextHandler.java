package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;

import static org.ethereum.beacon.discovery.pipeline.handler.IncomingDataHandler.UNKNOWN;
import static org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouContextHandler.CONTEXT;

public class IncomingPacketContextHandler implements EnvelopeHandler {
  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(CONTEXT)) {
      return;
    }
    if (!envelope.contains(UNKNOWN)) {
      return;
    }
    NodeContext context = (NodeContext) envelope.get(CONTEXT);
    UnknownPacket packet = (UnknownPacket) envelope.get(UNKNOWN);
    context.addIncomingEvent(packet);
  }
}
