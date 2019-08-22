package org.ethereum.beacon.types.p2p;

import tech.pegasys.artemis.util.bytes.ArrayWrappingBytesValue;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class NodeId extends ArrayWrappingBytesValue implements BytesValue {
  public NodeId(byte[] bytes) {
    super(bytes);
  }
}
