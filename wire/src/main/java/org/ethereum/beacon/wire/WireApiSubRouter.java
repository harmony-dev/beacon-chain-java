package org.ethereum.beacon.wire;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.stream.RxUtil;
import org.ethereum.beacon.util.Utils;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class WireApiSubRouter implements WireApiSub {
  private static final int DUPLICATE_DETECTION_SET_SIZE = 64;

  private final Flux<Attestation> inboundAttestationsStream;
  private final Flux<BeaconBlock> inboundBlocksStream;
  private FluxSink<Attestation> localAttestationSink;
  private FluxSink<BeaconBlock> localBlocksSink;

  private volatile List<WireApiSub> activeApis = Collections.emptyList();

  Set<BeaconBlock> seenBlocks;

  public WireApiSubRouter(
      Publisher<WireApiSub> addedPeersStream,
      Publisher<WireApiSub> removedPeersStream) {

    RxUtil.collect(addedPeersStream, removedPeersStream)
        .subscribe(l -> activeApis = l);

    // flood pub realization: upon receiving new block from remote or local
    // broadcasting it to all except sender
    // also filtering already known blocks
    Flux<BeaconBlock> localBlocks = Flux.create(e -> localBlocksSink = e);

    Flux<ApiData<BeaconBlock>> allNewBlocks = Flux.from(addedPeersStream)
        .flatMap(api -> Flux.from(api.inboundBlocksStream()).map(block -> new ApiData<>(api, block)))
        .mergeWith(localBlocks.map(block -> new ApiData<>(this, block)))
        .distinct(ApiData::getData, () -> seenBlocks = Utils.newLRUSet(DUPLICATE_DETECTION_SET_SIZE));

    allNewBlocks.subscribe(
        p -> {
          activeApis
              .stream()
              .filter(api -> !api.equals(p.getApi()))
              .forEach(api -> api.sendProposedBlock(p.getData()));
        });

    // the same flood pub for attestations
    Flux<Attestation> localAttestations = Flux.create(e -> localAttestationSink = e);

    Flux<ApiData<Attestation>> allNewAttestations = Flux.from(addedPeersStream)
        .flatMap(api -> Flux.from(api.inboundAttestationsStream()).map(attest -> new ApiData<>(api, attest)))
        .mergeWith(localAttestations.map(attest -> new ApiData<>(this, attest)))
        .distinct(ApiData::getData, () -> Utils.newLRUSet(DUPLICATE_DETECTION_SET_SIZE));

    allNewAttestations.subscribe(
        p -> {
          activeApis
              .stream()
              .filter(api -> !api.equals(p.getApi()))
              .forEach(api -> api.sendAttestation(p.getData()));
        });

    inboundBlocksStream = Flux.from(addedPeersStream)
        .flatMap(WireApiSub::inboundBlocksStream)
        .distinct(Function.identity(), () -> Utils.newLRUSet(DUPLICATE_DETECTION_SET_SIZE));

    inboundAttestationsStream = Flux
        .from(addedPeersStream)
        .flatMap(WireApiSub::inboundAttestationsStream)
        .distinct(Function.identity(), () -> Utils.newLRUSet(DUPLICATE_DETECTION_SET_SIZE));

  }

  @Override
  public void sendProposedBlock(BeaconBlock block) {
    localBlocksSink.next(block);
  }

  @Override
  public void sendAttestation(Attestation attestation) {
    localAttestationSink.next(attestation);
 }

  @Override
  public Publisher<BeaconBlock> inboundBlocksStream() {
    return inboundBlocksStream;
  }

  @Override
  public Publisher<Attestation> inboundAttestationsStream() {
    return inboundAttestationsStream;
  }

  static class ApiData<T> {
    private final WireApiSub api;
    private final T block;

    public ApiData(WireApiSub api, T block) {
      this.api = api;
      this.block = block;
    }

    public WireApiSub getApi() {
      return api;
    }

    public T getData() {
      return block;
    }
  }
}
