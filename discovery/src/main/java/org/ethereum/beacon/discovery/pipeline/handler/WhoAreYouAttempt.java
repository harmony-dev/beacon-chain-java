package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * Tries to get WHOAREYOU packet from unknown incoming packet in {@link Field#PACKET_UNKNOWN}. If it
 * was successful, places the result in {@link Field#PACKET_WHOAREYOU}
 */
public class WhoAreYouAttempt implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(WhoAreYouAttempt.class);
  private final Bytes32 homeNodeId;

  public WhoAreYouAttempt(Bytes32 homeNodeId) {
    this.homeNodeId = homeNodeId;
  }

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in WhoAreYouAttempt, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.PACKET_UNKNOWN, envelope)) {
      return;
    }
    if (!(HandlerUtil.requireCondition(
        envelope1 ->
            ((UnknownPacket) envelope1.get(Field.PACKET_UNKNOWN)).isWhoAreYouPacket(homeNodeId),
        envelope))) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in WhoAreYouAttempt, requirements are satisfied!", envelope.getId()));

    UnknownPacket unknownPacket = (UnknownPacket) envelope.get(Field.PACKET_UNKNOWN);
    envelope.put(Field.PACKET_WHOAREYOU, unknownPacket.getWhoAreYouPacket());
    envelope.remove(Field.PACKET_UNKNOWN);
  }
}
