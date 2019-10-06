package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * Assuming we have some unknown packet in {@link Field#PACKET_UNKNOWN}, resolves sender node id
 * using `tag` field of the packet. Next, puts it to the {@link Field#NEED_CONTEXT} so sender
 * context could be resolved by another handler.
 */
public class UnknownPacketTagToSender implements EnvelopeHandler {
  private final Bytes32 homeNodeId;

  public UnknownPacketTagToSender(NodeRecord homeNodeRecord) {
    this.homeNodeId = homeNodeRecord.getNodeId();
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_UNKNOWN)) {
      return;
    }
    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(Field.PACKET_UNKNOWN);
    Bytes32 fromNodeId = unknownPacket.getSourceNodeId(homeNodeId);
    envelope.put(
        Field.NEED_CONTEXT,
        Pair.with(
            fromNodeId,
            (Runnable)
                () -> {
                  envelope.put(Field.BAD_PACKET, envelope.get(Field.PACKET_UNKNOWN));
                  envelope.remove(Field.PACKET_UNKNOWN);
                }));
  }
}
