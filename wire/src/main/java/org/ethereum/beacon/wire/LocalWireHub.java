package org.ethereum.beacon.wire;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.wire.message.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class LocalWireHub {
  private class WireImpl implements WireApi {
    Processor<BeaconBlock, BeaconBlock> blocks =  DirectProcessor.create();
    Processor<Attestation, Attestation> attestations =  DirectProcessor.create();
    String name;
    long inboundDelay;
    long outboundDelay;

    public WireImpl(String name, long inboundDelay, long outboundDelay) {
      this.name = name;
      this.inboundDelay = inboundDelay;
      this.outboundDelay = outboundDelay;
    }

    @Override
    public void sendProposedBlock(BeaconBlock block) {
      if (outboundDelay == 0) {
        sendProposedBlockImpl(block);
      } else {
        schedulers.events().executeWithDelay(
            Duration.ofMillis(outboundDelay), () -> sendProposedBlockImpl(block));
      }
    }

    public void sendProposedBlockImpl(BeaconBlock block) {
      logger.accept("Node '" + name + "' => Block: " + block);
      for (WireImpl node : peers) {
        if (node != this) {
          node.blocks.onNext(block);
        }
      }
    }

    @Override
    public void sendAttestation(Attestation attestation) {
      if (outboundDelay == 0) {
        sendAttestationImpl(attestation);
      } else {
        schedulers.events().executeWithDelay(
            Duration.ofMillis(outboundDelay), () -> sendAttestationImpl(attestation));
      }
    }

    public void sendAttestationImpl(Attestation attestation) {
      logger.accept("Node '" + name + "' => Attestation: " + attestation);
      for (WireImpl node : peers) {
        if (node != this) {
          node.attestations.onNext(attestation);
        }
      }
    }

    @Override
    public Publisher<BeaconBlock> inboundBlocksStream() {
      if (inboundDelay == 0) {
        return blocks;
      } else {
        return Flux.from(blocks)
            .delayElements(Duration.ofMillis(inboundDelay), schedulers.reactorEvents());
      }
    }

    @Override
    public Publisher<Attestation> inboundAttestationsStream() {
      if (inboundDelay == 0) {
        return attestations;
      } else {
        return Flux.from(attestations)
            .delayElements(Duration.ofMillis(inboundDelay), schedulers.reactorEvents());
      }
    }

    @Override
    public Future<BlockRootsResponseMessage> requestBlockRoots(
        BlockHeadersRequestMessage requestMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<BlockHeadersResponseMessage> requestBlockHeaders(
        BlockRootsRequestMessage requestMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<BlockBodiesResponseMessage> requestBlockBodies(
        BlockBodiesRequestMessage requestMessage) {
      throw new UnsupportedOperationException();
    }
  }

  List<WireImpl> peers = new CopyOnWriteArrayList<>();
  Consumer<String> logger = s -> System.out.println(new Date() + ": " + s);
  Schedulers schedulers;

  public LocalWireHub(Consumer<String> logger, Schedulers schedulers) {
    this.logger = logger;
    this.schedulers = schedulers;
  }

  public WireApi createNewPeer(String name) {
    return createNewPeer(name, 0, 0);
  }

  public WireApi createNewPeer(String name, long inboundDelay, long outboundDelay) {
    WireImpl ret = new WireImpl(name, inboundDelay, outboundDelay);
    peers.add(ret);
    return ret;
  }
}
