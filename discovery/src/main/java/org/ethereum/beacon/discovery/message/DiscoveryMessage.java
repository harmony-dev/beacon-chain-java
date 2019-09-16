package org.ethereum.beacon.discovery.message;

import org.ethereum.beacon.discovery.IdentityScheme;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface DiscoveryMessage {
  IdentityScheme getIdentityScheme();

  BytesValue getBytes();
}
