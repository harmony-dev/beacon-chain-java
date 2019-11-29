package org.ethereum.beacon.discovery.message;

import org.ethereum.beacon.discovery.Protocol;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Discovery message */
public interface DiscoveryMessage {
  Protocol getProtocol();

  BytesValue getBytes();
}
