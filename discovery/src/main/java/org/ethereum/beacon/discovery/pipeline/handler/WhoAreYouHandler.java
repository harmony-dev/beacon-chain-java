package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import tech.pegasys.artemis.util.bytes.Bytes32;

import static org.ethereum.beacon.discovery.pipeline.handler.IncomingDataHandler.UNKNOWN;

public class WhoAreYouHandler implements EnvelopeHandler {
  public static final String WHOAREYOU = "WHOAREYOU";
  private final Bytes32 homeNodeId;

  public WhoAreYouHandler(Bytes32 homeNodeId) {
    this.homeNodeId = homeNodeId;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(UNKNOWN)) {
      return;
    }
    if (!((UnknownPacket) envelope.get(UNKNOWN)).isWhoAreYouPacket(homeNodeId)) {
      return;
    }
    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(UNKNOWN);
    envelope.put(WHOAREYOU, unknownPacket.getWhoAreYouPacket());
  }
}
