package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.packet.Packet;

public class NetworkPacketV5 {
  private final Packet packet;
  private final NodeRecordV5 nodeRecord;

  public NetworkPacketV5(Packet packet, NodeRecordV5 nodeRecord) {
    this.packet = packet;
    this.nodeRecord = nodeRecord;
  }

  public Packet getPacket() {
    return packet;
  }

  public NodeRecordV5 getNodeRecord() {
    return nodeRecord;
  }
}
