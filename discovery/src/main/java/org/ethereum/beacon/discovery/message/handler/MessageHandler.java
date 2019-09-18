package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;

public interface MessageHandler<Message> {
  void handle(Message message, NodeContext context);
}
