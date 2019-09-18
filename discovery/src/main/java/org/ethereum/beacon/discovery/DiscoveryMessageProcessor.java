package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.message.DiscoveryMessage;

public interface DiscoveryMessageProcessor<M extends DiscoveryMessage> {
  IdentityScheme getSupportedIdentity();

  void handleMessage(M message, NodeContext context);
}
