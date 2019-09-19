package org.ethereum.beacon.discovery.packet.handler;

import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.packet.MessagePacket;

public class MessagePacketHandler implements PacketHandler<MessagePacket> {
  private final NodeContext context;
  private final Logger logger;

  public MessagePacketHandler(NodeContext context, Logger logger) {
    this.context = context;
    this.logger = logger;
  }

  @Override
  public boolean handle(MessagePacket packet) {
    try {
      packet.decode(context.getInitiatorKey());
      packet.verify(context.getAuthTag().get());
      context.handleMessage(packet.getMessage());
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
