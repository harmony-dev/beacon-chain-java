package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.handler.FindNodeHandler;
import org.ethereum.beacon.discovery.message.handler.MessageHandler;
import org.ethereum.beacon.discovery.message.handler.NodesHandler;
import org.ethereum.beacon.discovery.message.handler.PingHandler;
import org.ethereum.beacon.discovery.message.handler.PongHandler;

import java.util.HashMap;
import java.util.Map;

public class DiscoveryV5MessageProcessor implements DiscoveryMessageProcessor<DiscoveryV5Message> {
  private final Map<MessageCode, MessageHandler> messageHandlers = new HashMap<>();

  public DiscoveryV5MessageProcessor() {
    messageHandlers.put(MessageCode.PING, new PingHandler());
    messageHandlers.put(MessageCode.PONG, new PongHandler());
    messageHandlers.put(MessageCode.FINDNODE, new FindNodeHandler());
    messageHandlers.put(MessageCode.NODES, new NodesHandler());
  }

  @Override
  public IdentityScheme getSupportedIdentity() {
    return IdentityScheme.V5;
  }

  @Override
  public void handleMessage(DiscoveryV5Message message, NodeSession session) {
    MessageCode code = message.getCode();
    MessageHandler messageHandler = messageHandlers.get(code);
    if (messageHandler == null) {
      throw new RuntimeException("Not implemented yet");
    }
    messageHandler.handle(message.create(), session);
  }
}
