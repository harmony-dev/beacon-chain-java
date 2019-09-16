package org.ethereum.beacon.discovery.message;

import tech.pegasys.artemis.util.bytes.BytesValue;

public interface V5Message {
  BytesValue getRequestId();
  BytesValue getBytes();
}
