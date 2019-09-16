package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.Packet;

public class NetworkPacket {
  private final Packet packet;
  private final NodeRecord nodeRecord;

  public NetworkPacket(Packet packet, NodeRecord nodeRecord) {
    this.packet = packet;
    this.nodeRecord = nodeRecord;
  }

  public Packet getPacket() {
    return packet;
  }

  public NodeRecord getNodeRecord() {
    return nodeRecord;
  }
}
