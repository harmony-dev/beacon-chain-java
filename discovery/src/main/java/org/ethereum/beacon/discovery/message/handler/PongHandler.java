package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;

public class PongHandler implements MessageHandler<PongMessage> {
  @Override
  public void handle(PongMessage message, NodeContext context) {
    context.clearRequestId(message.getRequestId(), MessageCode.PING);
  }
}
