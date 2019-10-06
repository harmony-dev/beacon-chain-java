package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Handles {@link UnknownPacket} from node, which is not on any stage of the handshake with us */
public class NotExpectedIncomingPacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(NotExpectedIncomingPacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_UNKNOWN)) {
      return;
    }
    if (!envelope.contains(Field.SESSION)) {
      return;
    }
    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(Field.PACKET_UNKNOWN);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    try {
      // packet it either random or message packet if session is expired
      BytesValue authTag = null;
      try {
        RandomPacket randomPacket = unknownPacket.getRandomPacket();
        authTag = randomPacket.getAuthTag();
      } catch (Exception ex) {
        // Not fatal, 1st attempt
      }
      // 2nd attempt
      if (authTag == null) {
        MessagePacket messagePacket = unknownPacket.getMessagePacket();
        authTag = messagePacket.getAuthTag();
      }
      session.setAuthTag(authTag);
      byte[] idNonceBytes = new byte[32];
      Functions.getRandom().nextBytes(idNonceBytes);
      Bytes32 idNonce = Bytes32.wrap(idNonceBytes);
      session.setIdNonce(idNonce);
      WhoAreYouPacket whoAreYouPacket =
          WhoAreYouPacket.create(
              session.getNodeRecord().getNodeId(),
              authTag,
              idNonce,
              session.getNodeRecord().getSeq());
      session.sendOutgoing(whoAreYouPacket);
    } catch (AssertionError ex) {
      logger.info(
          String.format(
              "Verification not passed for message [%s] from node %s in status %s",
              unknownPacket, session.getNodeRecord(), session.getStatus()));
    } catch (Exception ex) {
      String error =
          String.format(
              "Failed to read message [%s] from node %s in status %s",
              unknownPacket, session.getNodeRecord(), session.getStatus());
      logger.error(error, ex);
      envelope.put(Field.BAD_PACKET, envelope.get(Field.PACKET_UNKNOWN));
      envelope.put(Field.BAD_PACKET_EXCEPTION, ex);
      envelope.remove(Field.PACKET_UNKNOWN);
      return;
    }
    session.setStatus(NodeSession.SessionStatus.WHOAREYOU_SENT);
    envelope.remove(Field.PACKET_UNKNOWN);
  }
}
