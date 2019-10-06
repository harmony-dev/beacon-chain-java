package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.PongMessage;

public class PongHandler implements MessageHandler<PongMessage> {
  @Override
  public void handle(PongMessage message, NodeSession session) {
    session.clearRequestId(message.getRequestId(), MessageCode.PING);
  }
}
