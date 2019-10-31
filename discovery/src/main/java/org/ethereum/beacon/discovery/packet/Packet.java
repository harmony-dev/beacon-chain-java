package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Packet {
  static Bytes32 createTag(Bytes32 homeNodeId, Bytes32 destNodeId) {
    return Bytes32s.xor(Functions.hash(destNodeId), homeNodeId);
  }

  BytesValue getBytes();
}
