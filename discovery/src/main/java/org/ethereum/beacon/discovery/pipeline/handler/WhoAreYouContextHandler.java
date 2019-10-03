package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.Optional;

import static org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouHandler.WHOAREYOU;

public class WhoAreYouContextHandler implements EnvelopeHandler {
  public static final String CONTEXT = "context";
  public static final String BAD_PACKET = "BAD_PACKET";
  private final AuthTagRepository authTagRepo;

  public WhoAreYouContextHandler(AuthTagRepository authTagRepo) {
    this.authTagRepo = authTagRepo;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(WHOAREYOU)) {
      return;
    }

    WhoAreYouPacket whoAreYouPacket = (WhoAreYouPacket) envelope.get(WHOAREYOU);
    Optional<NodeContext> nodeContextOptional = authTagRepo.get(whoAreYouPacket.getAuthTag());
    if (nodeContextOptional.isPresent()) {
      envelope.put(CONTEXT, nodeContextOptional.get());
    } else {
      envelope.put(BAD_PACKET, envelope.get(WHOAREYOU));
      envelope.remove(WHOAREYOU);
    }
  }
}
