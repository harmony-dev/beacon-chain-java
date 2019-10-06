package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Handles raw BytesValue incoming data in {@link Field#INCOMING} */
public class IncomingDataPacker implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(IncomingDataPacker.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.INCOMING)) {
      return;
    }
    UnknownPacket unknownPacket = new UnknownPacket((BytesValue) envelope.get(Field.INCOMING));
    envelope.put(Field.PACKET_UNKNOWN, unknownPacket);
    logger.trace(
        () -> String.format("Incoming packet %s in envelope #%s", unknownPacket, envelope.getId()));
    envelope.remove(Field.INCOMING);
  }
}
