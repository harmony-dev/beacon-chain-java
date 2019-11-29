package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeSession;

public interface MessageHandler<Message> {
  void handle(Message message, NodeSession session);
}
