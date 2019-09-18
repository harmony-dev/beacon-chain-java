package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;

import java.util.HashMap;
import java.util.Map;

public class MessageProcessor {
  private static final Logger logger = LogManager.getLogger(MessageProcessor.class);
  private final Map<IdentityScheme, DiscoveryMessageProcessor> messageHandlers = new HashMap<>();

  public MessageProcessor(DiscoveryMessageProcessor... messageHandlers) {
    for (int i = 0; i < messageHandlers.length; ++i) {
      this.messageHandlers.put(messageHandlers[i].getSupportedIdentity(), messageHandlers[i]);
    }
  }

  public void handleIncoming(DiscoveryMessage message, NodeContext context) {
    IdentityScheme identityScheme = message.getIdentityScheme();
    DiscoveryMessageProcessor messageHandler = messageHandlers.get(identityScheme);
    if (messageHandler == null) {
      String error =
          String.format(
              "Message %s with identity %s received in context %s is not supported",
              message, identityScheme, context);
      logger.error(error);
      throw new RuntimeException(error);
    }
    messageHandler.handleMessage(message, context);
  }
}
