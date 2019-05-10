package org.ethereum.beacon.wire.sync;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.stream.RxUtil;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireInvalidConsensusDataException;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.sync.SyncQueue.BlockRequest;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class SyncManager {
  private static final Logger logger = LogManager.getLogger(SyncManager.class);

  private final MutableBeaconChain chain;
  private final Publisher<BeaconTupleDetails> blockStatesStream;
  private final BeaconChainStorage storage;
  private final BeaconChainSpec spec;

  private final WireApiSync syncApi;
  private Publisher<Feedback<BeaconBlock>> newBlocks;
  private final SyncQueue syncQueue;
  FluxSink<Publisher<BlockRequest>> requestsStreams;
  Flux<BlockRequest> blockRequestFlux;
  Scheduler delayScheduler;

  private Disposable wireBlocksStreamSub;
  private Disposable finalizedBlockStreamSub;
  private Disposable readyBlocksStreamSub;

  int maxConcurrentBlockRequests = 32;

  public SyncManager(MutableBeaconChain chain,
      Publisher<Feedback<BeaconBlock>> newBlocks,
      BeaconChainStorage storage, BeaconChainSpec spec, WireApiSync syncApi,
      SyncQueue syncQueue, int maxConcurrentBlockRequests,
      Scheduler delayScheduler) {
    this.chain = chain;
    this.blockStatesStream = chain.getBlockStatesStream();
    this.newBlocks = newBlocks;
    this.storage = storage;
    this.spec = spec;
    this.syncApi = syncApi;
    this.syncQueue = syncQueue;
    this.maxConcurrentBlockRequests = maxConcurrentBlockRequests;
    this.delayScheduler = delayScheduler;

    ModeDetector modeDetector = new ModeDetector(
        Flux.from(chain.getBlockStatesStream()).map(BeaconTuple::getBlock),
        Flux.from(newBlocks).map(Feedback::get));
    blockRequestFlux = Flux.from(modeDetector.getSyncModeStream())
        .doOnNext(mode -> logger.info("Switch sync to mode " + mode))
        .switchMap(
            mode -> {
              switch (mode) {
                case Long:
                  return syncQueue.getBlockRequestsStream();
                case Short:
                  return Flux.from(syncQueue.getBlockRequestsStream())
                      .delayElements(Duration.ofSeconds(1), delayScheduler);
                default:
                  throw new IllegalStateException();
              }
            });
  }

  public void start() {

    Hash32 genesisBlockRoot =
        storage.getBlockStorage().getSlotBlocks(spec.getConstants().getGenesisSlot()).get(0);

    Flux<Hash32> finalizedBlockRootStream = Flux
        .from(blockStatesStream)
        .map(bs -> bs.getFinalState().getFinalizedRoot())
        .distinct()
        .map(br -> Hash32.ZERO.equals(br) ? genesisBlockRoot : br);

    Flux<BeaconBlock> finalizedBlockStream = finalizedBlockRootStream.map(
            root -> storage.getBlockStorage().get(root).orElseThrow(() -> new IllegalStateException()));

    finalizedBlockStreamSub = syncQueue.subscribeToFinalBlocks(finalizedBlockStream);

    readyBlocksStreamSub = Flux.from(syncQueue.getBlocksStream()).subscribe(block -> {
      if (!chain.insert(block.get())) {
        block.feedbackError(
            new WireInvalidConsensusDataException("Couldn't insert block: " + block.get()));
      } else {
        block.feedbackSuccess();
      }
    });

    Flux<Feedback<List<BeaconBlock>>> wireBlocksStream = blockRequestFlux
        .map(req -> new BlockHeadersRequestMessage(
            req.getStartRoot().orElse(BlockHeadersRequestMessage.NULL_START_ROOT),
            req.getStartSlot().orElse(BlockHeadersRequestMessage.NULL_START_SLOT),
            req.getMaxCount(),
            req.getStep()))
        .flatMap(req -> Mono.fromFuture(syncApi.requestBlocks(req, spec.getObjectHasher())),
            maxConcurrentBlockRequests)
        .onErrorContinue((t, o) -> logger.info("SyncApi exception: " + t + ", " + o, t));

    if (newBlocks != null) {
      wireBlocksStream = wireBlocksStream.mergeWith(
          Flux.from(newBlocks).map(blockF -> blockF.map(Collections::singletonList)));
    }

    wireBlocksStreamSub = syncQueue.subscribeToNewBlocks(wireBlocksStream);
  }

  public void stop() {
    wireBlocksStreamSub.dispose();
    finalizedBlockStreamSub.dispose();
    readyBlocksStreamSub.dispose();
  }

  enum SyncMode {
    Long,
    Short
  }

  class ModeDetector {
    Publisher<SyncMode> syncModeStream;

    public ModeDetector(
        Publisher<BeaconBlock> importedBlocks,
        Publisher<BeaconBlock> onlineBlocks) {

      syncModeStream = Flux.combineLatest(
          Flux.from(importedBlocks).
              scan(new ArrayList<>(), (arr, block) -> listAddLimited(arr, block, 8)),
          Flux.from(onlineBlocks).
              scan(new ArrayList<>(), (arr, block) -> listAddLimited(arr, block, 8)),
          (latestImported, latestOnline) -> {
            HashSet<?> s1 = new HashSet<>(latestImported);
            HashSet<?> s2 = new HashSet<>(latestOnline);
            s1.retainAll(s2);
            return s1.isEmpty() ? SyncMode.Long : SyncMode.Short;
          });
    }

    private <A> ArrayList<A> listAddLimited(ArrayList<A> list, A elem, int maxSize) {
      list.add(elem);
      if (list.size() > maxSize) {
        list.remove(0);
      }
      return list;
    }

    public Publisher<SyncMode> getSyncModeStream() {
      return syncModeStream;
    }
  }
}
