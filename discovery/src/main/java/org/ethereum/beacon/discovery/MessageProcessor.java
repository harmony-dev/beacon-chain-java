package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Highest level processor which knows several processors for different versions of {@link
 * DiscoveryMessage}'s.
 */
public class MessageProcessor {
  private static final Logger logger = LogManager.getLogger(MessageProcessor.class);
  private final Map<Protocol, DiscoveryMessageProcessor> messageProcessors = new HashMap<>();

  public MessageProcessor(DiscoveryMessageProcessor... messageProcessors) {
    for (int i = 0; i < messageProcessors.length; ++i) {
      this.messageProcessors.put(messageProcessors[i].getSupportedIdentity(), messageProcessors[i]);
    }
  }

  public void handleIncoming(DiscoveryMessage message, NodeSession session) {
    Protocol protocol = message.getProtocol();
    DiscoveryMessageProcessor messageHandler = messageProcessors.get(protocol);
    if (messageHandler == null) {
      String error =
          String.format(
              "Message %s with identity %s received in session %s is not supported",
              message, protocol, session);
      logger.error(error);
      throw new RuntimeException(error);
    }
    messageHandler.handleMessage(message, session);
  }
}
