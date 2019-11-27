package org.ethereum.beacon.discovery.network;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Discovery client sends outgoing messages */
public interface DiscoveryClient {
  void stop();

  void send(BytesValue data, NodeRecord recipient);
}
