package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;

/** Logs all packets which are stored in {@link Field#BAD_PACKET} */
public class BadPacketLogger implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(BadPacketLogger.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.BAD_PACKET)) {
      return;
    }
    logger.debug(
        () ->
            String.format(
                "Bad packet: %s in envelope #%s", envelope.get(Field.BAD_PACKET), envelope.getId()),
        (Exception) envelope.get(Field.BAD_PACKET_EXCEPTION));
  }
}
