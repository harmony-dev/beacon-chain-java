package org.ethereum.beacon.discovery.packet;

import tech.pegasys.artemis.util.bytes.BytesValue;

public abstract class AbstractPacket implements Packet {
  private final BytesValue bytes;

  AbstractPacket(BytesValue bytes) {
    this.bytes = bytes;
  }

  @Override
  public BytesValue getBytes() {
    return bytes;
  }
}
