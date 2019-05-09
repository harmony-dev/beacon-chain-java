package org.ethereum.beacon.wire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class WireApiSubRouter implements WireApiSub {

  private final List<WireApiSub> activeApis = new CopyOnWriteArrayList<>();

  private final Flux<Attestation> attestationsStream;
  private final Flux<BeaconBlock> blocksStream;

  public WireApiSubRouter(
      Publisher<WireApiSub> addedPeersStream,
      Publisher<WireApiSub> removedPeersStream) {

    blocksStream = Flux
        .from(addedPeersStream)
        .flatMap(WireApiSub::inboundBlocksStream)
        .distinct();

    attestationsStream = Flux
        .from(addedPeersStream)
        .flatMap(WireApiSub::inboundAttestationsStream)
        .distinct();

    Flux.from(addedPeersStream).subscribe(api -> activeApis.add(api));
    Flux.from(removedPeersStream).subscribe(api -> activeApis.remove(api));
  }

  @Override
  public void sendProposedBlock(BeaconBlock block) {
    activeApis.forEach(api -> api.sendProposedBlock(block));
  }

  @Override
  public void sendAttestation(Attestation attestation) {
    activeApis.forEach(api -> api.sendAttestation(attestation));
 }

  @Override
  public Publisher<BeaconBlock> inboundBlocksStream() {
    return blocksStream;
  }

  @Override
  public Publisher<Attestation> inboundAttestationsStream() {
    return attestationsStream;
  }
}
