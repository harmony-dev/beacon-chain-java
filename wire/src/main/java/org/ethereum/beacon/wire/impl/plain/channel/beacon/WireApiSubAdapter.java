package org.ethereum.beacon.wire.impl.plain.channel.beacon;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.wire.WireApiSub;
import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;

public class WireApiSubAdapter implements WireApiSub, WireApiSubRpc {
  private final ReplayProcessor<SignedBeaconBlock> blockProcessor = ReplayProcessor.cacheLast();
  private final FluxSink<SignedBeaconBlock> blockSink = blockProcessor.sink();
  private final ReplayProcessor<Attestation> attestProcessor = ReplayProcessor.cacheLast();
  private final FluxSink<Attestation> attestSink = attestProcessor.sink();

  private WireApiSubRpc subClient;

  public void setSubClient(WireApiSubRpc subClient) {
    this.subClient = subClient;
  }

  @Override
  public void sendProposedBlock(SignedBeaconBlock block) {
    subClient.newBlock(block);
  }

  @Override
  public void sendAttestation(Attestation attestation) {
    subClient.newAttestation(attestation);
  }

  @Override
  public Publisher<SignedBeaconBlock> inboundBlocksStream() {
    return blockProcessor;
  }

  @Override
  public Publisher<Attestation> inboundAttestationsStream() {
    return attestProcessor;
  }

  @Override
  public void newBlock(SignedBeaconBlock block) {
    blockSink.next(block);
  }

  @Override
  public void newAttestation(Attestation attestation) {
    attestSink.next(attestation);
  }
}
