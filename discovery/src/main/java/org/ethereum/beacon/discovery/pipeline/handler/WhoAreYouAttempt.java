package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * Tries to get WHOAREYOU packet from unknown incoming packet in {@link Field#PACKET_UNKNOWN}. If it
 * was successful, places the result in {@link Field#PACKET_WHOAREYOU}
 */
public class WhoAreYouAttempt implements EnvelopeHandler {
  private final Bytes32 homeNodeId;

  public WhoAreYouAttempt(Bytes32 homeNodeId) {
    this.homeNodeId = homeNodeId;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_UNKNOWN)) {
      return;
    }
    if (!((UnknownPacket) envelope.get(Field.PACKET_UNKNOWN)).isWhoAreYouPacket(homeNodeId)) {
      return;
    }
    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(Field.PACKET_UNKNOWN);
    envelope.put(Field.PACKET_WHOAREYOU, unknownPacket.getWhoAreYouPacket());
    envelope.remove(Field.PACKET_UNKNOWN);
  }
}
