package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Network packet as defined by discovery v5 specification. See <a
 * href="https://github.com/ethereum/devp2p/blob/master/discv5/discv5-wire.md#packet-encoding">https://github.com/ethereum/devp2p/blob/master/discv5/discv5-wire.md#packet-encoding</a>
 */
public interface Packet {
  static Bytes32 createTag(Bytes32 homeNodeId, Bytes32 destNodeId) {
    return Bytes32s.xor(Functions.hash(destNodeId), homeNodeId);
  }

  BytesValue getBytes();
}
