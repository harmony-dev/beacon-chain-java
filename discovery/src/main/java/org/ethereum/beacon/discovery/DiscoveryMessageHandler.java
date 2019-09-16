package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.message.DiscoveryMessage;

public interface DiscoveryMessageHandler<M extends DiscoveryMessage> {
  void handleMessage(M message, NodeContext context);
}
