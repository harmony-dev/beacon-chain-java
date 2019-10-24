package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
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
  private static final Logger logger = LogManager.getLogger(DiscoveryV5MessageProcessor.class);
  private final Map<MessageCode, MessageHandler> messageHandlers = new HashMap<>();
  private final NodeRecordFactory nodeRecordFactory;

  public DiscoveryV5MessageProcessor(NodeRecordFactory nodeRecordFactory) {
    messageHandlers.put(MessageCode.PING, new PingHandler());
    messageHandlers.put(MessageCode.PONG, new PongHandler());
    messageHandlers.put(MessageCode.FINDNODE, new FindNodeHandler());
    messageHandlers.put(MessageCode.NODES, new NodesHandler());
    this.nodeRecordFactory = nodeRecordFactory;
  }

  @Override
  public IdentityScheme getSupportedIdentity() {
    return IdentityScheme.V5;
  }

  @Override
  public void handleMessage(DiscoveryV5Message message, NodeSession session) {
    MessageCode code = message.getCode();
    MessageHandler messageHandler = messageHandlers.get(code);
    logger.trace(() -> String.format("Handling message %s in session %s", message, session));
    if (messageHandler == null) {
      throw new RuntimeException("Not implemented yet");
    }
    messageHandler.handle(message.create(nodeRecordFactory), session);
  }
}
