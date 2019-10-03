package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.Bytes32;

import static org.ethereum.beacon.discovery.pipeline.handler.IncomingDataHandler.UNKNOWN;
import static org.ethereum.beacon.discovery.pipeline.handler.NodeIdContextHandler.NEED_CONTEXT;
import static org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouContextHandler.BAD_PACKET;

public class UnknownPacketContextHandler implements EnvelopeHandler {
  private static final int CLEANUP_DELAY_SECONDS = 180;
  private final Bytes32 homeNodeId;

  public UnknownPacketContextHandler(NodeRecord homeNodeRecord) {
    this.homeNodeId = homeNodeRecord.getNodeId();
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(UNKNOWN)) {
      return;
    }
    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(UNKNOWN);
    Bytes32 fromNodeId = unknownPacket.getSourceNodeId(homeNodeId);
    envelope.put(
        NEED_CONTEXT,
        Pair.with(
            fromNodeId,
            (Runnable)
                () -> {
                  envelope.put(BAD_PACKET, envelope.get(UNKNOWN));
                  envelope.remove(UNKNOWN);
                }));
  }
}
