package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Handles raw BytesValue incoming data in {@link Field#INCOMING} */
public class IncomingDataPacker implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(IncomingDataPacker.class);

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in IncomingDataPacker, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.INCOMING, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in IncomingDataPacker, requirements are satisfied!",
                envelope.getId()));

    UnknownPacket unknownPacket = new UnknownPacket((BytesValue) envelope.get(Field.INCOMING));
    try {
      unknownPacket.verify();
      envelope.put(Field.PACKET_UNKNOWN, unknownPacket);
      logger.trace(
          () ->
              String.format("Incoming packet %s in envelope #%s", unknownPacket, envelope.getId()));
    } catch(Exception ex) {
      envelope.put(Field.BAD_PACKET, unknownPacket);
      envelope.put(Field.BAD_EXCEPTION, ex);
      envelope.put(Field.BAD_MESSAGE, "Incoming packet verification not passed");
      logger.trace(
          () ->
              String.format("Bad incoming packet %s in envelope #%s", unknownPacket, envelope.getId()));
    }
    envelope.remove(Field.INCOMING);
  }
}
