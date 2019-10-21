package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;

/** Logs all packets which are stored in {@link Field#BAD_PACKET} */
public class BadPacketLogger implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(BadPacketLogger.class);

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in BadPacketLogger, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.BAD_PACKET, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in BadPacketLogger, requirements are satisfied!", envelope.getId()));

    logger.debug(
        () ->
            String.format(
                "Bad packet: %s in envelope #%s", envelope.get(Field.BAD_PACKET), envelope.getId()),
        (Exception) envelope.get(Field.BAD_EXCEPTION));
  }
}
