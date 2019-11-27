package org.ethereum.beacon.discovery.network;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.Packet;

public class NetworkParcelV5 implements NetworkParcel {
  private final Packet packet;
  private final NodeRecord nodeRecord;

  public NetworkParcelV5(Packet packet, NodeRecord nodeRecord) {
    this.packet = packet;
    this.nodeRecord = nodeRecord;
  }

  @Override
  public Packet getPacket() {
    return packet;
  }

  @Override
  public NodeRecord getNodeRecord() {
    return nodeRecord;
  }
}
