package org.ethereum.beacon.discovery.network;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.Packet;

/**
 * Abstraction on the top of the {@link Packet}.
 *
 * <p>Stores `packet` and associated node record. Record could be a sender or recipient, depends on
 * session.
 */
public interface NetworkParcel {
  Packet getPacket();

  NodeRecord getNodeRecord();
}
