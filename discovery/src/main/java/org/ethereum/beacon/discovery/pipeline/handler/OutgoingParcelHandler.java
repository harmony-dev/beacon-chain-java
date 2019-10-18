package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import reactor.core.publisher.FluxSink;

/**
 * Looks up for {@link NetworkParcel} in {@link Field#INCOMING} field. If it's found, it shows that
 * we have outgoing parcel at the very first stage. Handler pushes it to `outgoingSink` stream which
 * is linked with discovery client.
 */
public class OutgoingParcelHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(OutgoingParcelHandler.class);

  private final FluxSink<NetworkParcel> outgoingSink;

  public OutgoingParcelHandler(FluxSink<NetworkParcel> outgoingSink) {
    this.outgoingSink = outgoingSink;
  }

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in OutgoingParcelHandler, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.INCOMING, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in OutgoingParcelHandler, requirements are satisfied!",
                envelope.getId()));

    if (envelope.get(Field.INCOMING) instanceof NetworkParcel) {
      outgoingSink.next((NetworkParcel) envelope.get(Field.INCOMING));
      envelope.remove(Field.INCOMING);
    }
  }
}
