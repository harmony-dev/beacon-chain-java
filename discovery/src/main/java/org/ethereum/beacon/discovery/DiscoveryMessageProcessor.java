package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.message.DiscoveryMessage;

/** Handles discovery messages of several types */
public interface DiscoveryMessageProcessor<M extends DiscoveryMessage> {
  Protocol getSupportedIdentity();

  void handleMessage(M message, NodeSession session);
}
