package org.ethereum.beacon.discovery.packet.handler;

import org.ethereum.beacon.discovery.packet.Packet;

public interface PacketHandler<P extends Packet> {
  boolean handle(P packet);
}
