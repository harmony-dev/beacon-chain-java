package org.ethereum.beacon.discovery.packet.handler;

import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class UnknownPacketHandler implements PacketHandler<UnknownPacket> {
  private final NodeContext context;
  private final Logger logger;

  public UnknownPacketHandler(NodeContext context, Logger logger) {
    this.context = context;
    this.logger = logger;
  }

  @Override
  public boolean handle(UnknownPacket packet) {
    try {
      // packet it either random or message packet if session is expired
      BytesValue authTag = null;
      try {
        RandomPacket randomPacket = packet.getRandomPacket();
        authTag = randomPacket.getAuthTag();
      } catch (Exception ex) {
        // Not fatal, 1st attempt
      }
      // 2nd attempt
      if (authTag == null) {
        MessagePacket messagePacket = packet.getMessagePacket();
        authTag = messagePacket.getAuthTag();
      }
      context.setAuthTag(authTag);
      byte[] idNonceBytes = new byte[32];
      Functions.getRandom().nextBytes(idNonceBytes);
      Bytes32 idNonce = Bytes32.wrap(idNonceBytes);
      context.setIdNonce(idNonce);
      WhoAreYouPacket whoAreYouPacket =
          WhoAreYouPacket.create(
              context.getNodeRecord().getNodeId(),
              authTag,
              idNonce,
              context.getNodeRecord().getSeq());
      context.addOutgoingEvent(whoAreYouPacket);
    } catch (AssertionError ex) {
      logger.info(
          String.format(
              "Verification not passed for message [%s] from node %s in status %s",
              packet, context.getNodeRecord(), context.getStatus()));
    } catch (Exception ex) {
      String error =
          String.format(
              "Failed to read message [%s] from node %s in status %s",
              packet, context.getNodeRecord(), context.getStatus());
      logger.error(error, ex);
      return false;
    }
    return true;
  }
}
