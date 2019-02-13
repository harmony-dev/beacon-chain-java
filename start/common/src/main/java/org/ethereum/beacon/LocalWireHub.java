package org.ethereum.beacon;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;

public class LocalWireHub {
  private class WireImpl implements WireApi {
    Processor<BeaconBlock, BeaconBlock> blocks =  DirectProcessor.create();
    Processor<Attestation, Attestation> attestations =  DirectProcessor.create();
    String name;

    public WireImpl(String name) {
      this.name = name;
    }

    @Override
    public void sendProposedBlock(BeaconBlock block) {
      logger.accept("Node '" + name + "' => Block: " + block);
      for (WireImpl node : peers) {
        if (node != this) {
          node.blocks.onNext(block);
        }
      }
    }

    @Override
    public void sendAttestation(Attestation attestation) {
      logger.accept("Node '" + name + "' => Attestation: " + attestation);
      for (WireImpl node : peers) {
        if (node != this) {
          node.attestations.onNext(attestation);
        }
      }
    }

    @Override
    public Publisher<BeaconBlock> inboundBlocksStream() {
      return blocks;
    }

    @Override
    public Publisher<Attestation> inboundAttestationsStream() {
      return attestations;
    }
  }

  List<WireImpl> peers = new CopyOnWriteArrayList<>();
  Consumer<String> logger = s -> {};

  public LocalWireHub(Consumer<String> logger) {
    this.logger = logger;
  }

  public WireApi createNewPeer(String name) {
    WireImpl ret = new WireImpl(name);
    peers.add(ret);
    return ret;
  }
}
