package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;

public class MessageHandler {
  private static final Logger logger = LogManager.getLogger(MessageHandler.class);
  private final DiscoveryV5MessageHandler v5MessageHandler;

  public MessageHandler(DiscoveryV5MessageHandler v5MessageHandler) {
    this.v5MessageHandler = v5MessageHandler;
  }

  public void handleIncoming(DiscoveryMessage message, NodeContext context) {
    IdentityScheme identityScheme = message.getIdentityScheme();
    switch (identityScheme) {
      case V5:
        {
          v5MessageHandler.handleMessage((DiscoveryV5Message) message, context);
        }
      default:
        {
          String error =
              String.format(
                  "Message %s with identity %s received in context %s is not supported",
                  message, identityScheme, context);
          logger.error(error);
          throw new RuntimeException(error);
        }
    }
  }
}
