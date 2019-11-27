package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;

/**
 * Resolves incoming packet type based on session states and places packet into the corresponding
 * field. Doesn't recognize WhoAreYou packet, last should be resolved by {@link WhoAreYouAttempt}
 */
public class UnknownPacketTypeByStatus implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(UnknownPacketTypeByStatus.class);

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in UnknownPacketTypeByStatus, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.SESSION, envelope)) {
      return;
    }
    if (!HandlerUtil.requireField(Field.PACKET_UNKNOWN, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in UnknownPacketTypeByStatus, requirements are satisfied!", envelope.getId()));

    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(Field.PACKET_UNKNOWN);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    switch (session.getStatus()) {
      case INITIAL:
        {
          // We still don't know what's the type of the packet
          break;
        }
      case RANDOM_PACKET_SENT:
        {
          // Should receive WHOAREYOU in answer, not our case
          break;
        }
      case WHOAREYOU_SENT:
        {
          AuthHeaderMessagePacket authHeaderMessagePacket =
              unknownPacket.getAuthHeaderMessagePacket();
          envelope.put(Field.PACKET_AUTH_HEADER_MESSAGE, authHeaderMessagePacket);
          envelope.remove(Field.PACKET_UNKNOWN);
          break;
        }
      case AUTHENTICATED:
        {
          MessagePacket messagePacket = unknownPacket.getMessagePacket();
          envelope.put(Field.PACKET_MESSAGE, messagePacket);
          envelope.remove(Field.PACKET_UNKNOWN);
          break;
        }
      default:
        {
          String error =
              String.format(
                  "Not expected status:%s from node: %s",
                  session.getStatus(), session.getNodeRecord());
          logger.error(error);
          throw new RuntimeException(error);
        }
    }
  }
}
