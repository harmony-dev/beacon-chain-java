package org.ethereum.beacon.discovery.message;

import tech.pegasys.artemis.util.bytes.BytesValue;

/** Message of V5 discovery protocol version */
public interface V5Message {
  BytesValue getRequestId();

  BytesValue getBytes();
}
