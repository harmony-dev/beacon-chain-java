package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;

import java.util.Optional;

/**
 * Resolves context using `authTagRepo` for `WHOAREYOU` packets which should be placed in {@link
 * Field#PACKET_WHOAREYOU}
 */
public class WhoAreYouContextResolver implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(WhoAreYouContextResolver.class);
  private final AuthTagRepository authTagRepo;

  public WhoAreYouContextResolver(AuthTagRepository authTagRepo) {
    this.authTagRepo = authTagRepo;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_WHOAREYOU)) {
      return;
    }

    WhoAreYouPacket whoAreYouPacket = (WhoAreYouPacket) envelope.get(Field.PACKET_WHOAREYOU);
    Optional<NodeContext> nodeContextOptional = authTagRepo.get(whoAreYouPacket.getAuthTag());
    if (nodeContextOptional.isPresent()
        && nodeContextOptional
            .get()
            .getStatus()
            .equals(
                NodeContext.SessionStatus
                    .RANDOM_PACKET_SENT)) { // FIXME: doesn't handle session expiration
      envelope.put(Field.CONTEXT, nodeContextOptional.get());
      logger.trace(
          () ->
              String.format(
                  "Context resolved: %s in envelope #%s",
                  nodeContextOptional.get(), envelope.getId()));
    } else {
      envelope.put(Field.BAD_PACKET, envelope.get(Field.PACKET_WHOAREYOU));
      envelope.remove(Field.PACKET_WHOAREYOU);
      envelope.put(
          Field.BAD_PACKET_EXCEPTION, new RuntimeException("Not expected WHOAREYOU packet"));
    }
  }
}
