package org.ethereum.beacon.wire.sync;

import static java.lang.Math.max;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.ExistingBlock;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.ExpiredBlock;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.InvalidBlock;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.NoParent;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.OK;
import static org.ethereum.beacon.chain.MutableBeaconChain.ImportResult.StateMismatch;
import static org.ethereum.beacon.stream.RxUtil.fromOptional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain.ImportResult;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireInvalidConsensusDataException;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRequestMessage;
import org.ethereum.beacon.wire.sync.SyncQueue.BlockRequest;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64s;

public class SyncManagerImpl implements SyncManager {

  private static final Logger logger = LogManager.getLogger(SyncManagerImpl.class);

  private final Publisher<BeaconTupleDetails> blockStatesStream;
  private final BeaconChainSpec spec;
  private final WireApiSync syncApi;
  private final SyncQueue syncQueue;
  private final ModeDetector modeDetector;
  private final Flux<SyncMode> syncModeFlux;
  private final SimpleProcessor<Boolean> isSyncingProcessor;
  private final SimpleProcessor<SlotNumber> startSlotProcessor;
  private final Flux<SlotNumber> lastSlotFlux;
  FluxSink<Publisher<BlockRequest>> requestsStreams;
  Flux<BlockRequest> blockRequestFlux;
  Flux<BeaconBlock> finalizedBlockStream;
  // TODO: make this parameter dynamic depending on active peers number
  int maxConcurrentBlockRequests = 2;
  private Publisher<Feedback<BeaconBlock>> newBlocks;
  private Disposable wireBlocksStreamSub;
  private Disposable finalizedBlockStreamSub;
  private Disposable readyBlocksStreamSub;
  private Duration requestsDelayLongMode = Duration.ZERO;
  private Duration requestsDelayShortMode = Duration.ofSeconds(1);

  public SyncManagerImpl(
      MutableBeaconChain chain,
      Publisher<Feedback<BeaconBlock>> newBlocks,
      BeaconChainStorage storage,
      BeaconChainSpec spec,
      WireApiSync syncApi,
      SyncQueue syncQueue,
      int maxConcurrentBlockRequests,
      Schedulers schedulers) {

    this.blockStatesStream = chain.getBlockStatesStream();
    this.newBlocks = newBlocks;
    this.spec = spec;
    this.syncApi = syncApi;
    this.syncQueue = syncQueue;
    this.maxConcurrentBlockRequests = maxConcurrentBlockRequests;

    modeDetector =
        new ModeDetector(
            Flux.from(chain.getBlockStatesStream()).map(BeaconTuple::getBlock),
            Flux.from(newBlocks).map(Feedback::get));
    syncModeFlux = Flux.from(modeDetector.getSyncModeStream()).replay(1).autoConnect();
    final Scheduler delayScheduler = schedulers.events();
    blockRequestFlux =
        syncModeFlux
            .doOnNext(mode -> logger.info("Switch sync to mode " + mode))
            .switchMap(
                mode -> {
                  switch (mode) {
                    case Long:
                      Flux<BlockRequest> blockRequestFlux =
                          Flux.from(syncQueue.getBlockRequestsStream());
                      return requestsDelayLongMode.toMillis() == 0
                          ? blockRequestFlux
                          : blockRequestFlux.delayElements(
                              requestsDelayLongMode, delayScheduler.toReactor());
                    case Short:
                      return Flux.from(syncQueue.getBlockRequestsStream())
                          .delayElements(requestsDelayShortMode, delayScheduler.toReactor());
                    default:
                      throw new IllegalStateException();
                  }
                },
                1);

    Hash32 genesisBlockRoot =
        storage.getBlockStorage().getSlotBlocks(spec.getConstants().getGenesisSlot()).get(0);

    Flux<Hash32> finalizedBlockRootStream =
        Flux.from(blockStatesStream)
            .map(bs -> bs.getFinalState().getFinalizedCheckpoint().getRoot())
            .distinct()
            .map(br -> Hash32.ZERO.equals(br) ? genesisBlockRoot : br);

    finalizedBlockStream =
        finalizedBlockRootStream.map(
            root ->
                storage.getBlockStorage().get(root).orElseThrow(() -> new IllegalStateException()));

    readyBlocksStreamSub =
        Flux.from(syncQueue.getBlocksStream())
            .subscribe(
                block -> {
                  ImportResult result = chain.insert(block.get());
                  if (result == InvalidBlock || result == StateMismatch || result == ExpiredBlock) {
                    block.feedbackError(
                        new WireInvalidConsensusDataException(
                            "Couldn't insert block: " + block.get()));
                  } else {
                    block.feedbackSuccess();
                    if (result == NoParent) {
                      logger.warn("No parent for block: " + block.get());
                    } else if (result == ExistingBlock) {
                      logger.info("Trying to import existing block: " + block.get());
                    } else if (result != OK) {
                      logger.info("Other error importing block: " + block.get());
                    }
                  }
                });

    isSyncingProcessor = new SimpleProcessor<>(delayScheduler, "SyncManager.isSyncing", false);
    startSlotProcessor =
        new SimpleProcessor<>(
            delayScheduler,
            "SyncManager.startSlot",
            chain.getRecentlyProcessed().getBlock().getSlot());
    lastSlotFlux =
        Flux.from(blockStatesStream)
            .flatMap(s -> fromOptional(s.getPostSlotState()))
            .map(BeaconState::getSlot)
            .scan(UInt64s::max)
            .distinctUntilChanged();
  }

  @Override
  public Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport() {
    return syncQueue.getBlocksStream();
  }

  public void setRequestsDelay(Duration longMode, Duration shortMode) {
    this.requestsDelayLongMode = longMode;
    this.requestsDelayShortMode = shortMode;
  }

  @Override
  public void start() {

    finalizedBlockStreamSub = syncQueue.subscribeToFinalBlocks(finalizedBlockStream);

    Flux<Feedback<List<BeaconBlock>>> wireBlocksStream =
        blockRequestFlux
            .map(
                req ->
                    new BlockRequestMessage(
                        req.getStartRoot().orElse(BlockHeadersRequestMessage.NULL_START_ROOT),
                        req.getStartSlot().orElse(BlockHeadersRequestMessage.NULL_START_SLOT),
                        req.getMaxCount(),
                        req.getStep()))
            .flatMap(
                req -> Mono.fromFuture(syncApi.requestBlocks(req, spec.getObjectHasher())),
                maxConcurrentBlockRequests)
            .onErrorContinue((t, o) -> {
              logger.warn("SyncApi exception: " + t + ", " + o);
              logger.debug(t);
            });

    if (newBlocks != null) {
      wireBlocksStream =
          wireBlocksStream.mergeWith(
              Flux.from(newBlocks).map(blockF -> blockF.map(Collections::singletonList)));
    }

    wireBlocksStreamSub = syncQueue.subscribeToNewBlocks(wireBlocksStream);

    isSyncingProcessor.onNext(true);
  }

  @Override
  public void stop() {
    wireBlocksStreamSub.dispose();
    finalizedBlockStreamSub.dispose();
    readyBlocksStreamSub.dispose();
    isSyncingProcessor.onNext(false);
  }

  @Override
  public Publisher<SyncMode> getSyncModeStream() {
    return syncModeFlux;
  }

  @Override
  public Publisher<Boolean> getIsSyncingStream() {
    return isSyncingProcessor;
  }

  @Override
  public Publisher<SlotNumber> getStartSlotStream() {
    return startSlotProcessor;
  }

  @Override
  public Publisher<SlotNumber> getLastSlotStream() {
    return lastSlotFlux;
  }

  @Override
  public Disposable subscribeToOnlineBlocks(Publisher<Feedback<BeaconBlock>> onlineBlocks) {
    throw new RuntimeException("Not implemented yet!");
  }

  @Override
  public Disposable subscribeToFinalizedBlocks(Publisher<BeaconBlock> finalBlocks) {
    throw new RuntimeException("Not implemented yet!");
  }

  @Override
  public void setSyncApi(WireApiSync syncApi) {
    throw new RuntimeException("Not implemented yet!");
  }

  class ModeDetector {
    Publisher<SyncMode> syncModeStream;

    public ModeDetector(
        Publisher<BeaconBlock> importedBlocks, Publisher<BeaconBlock> onlineBlocks) {

      syncModeStream =
          Flux.combineLatest(
                  Flux.from(importedBlocks)
                      .scan(new ArrayList<>(), (arr, block) -> listAddLimited(arr, block, 8)),
                  Flux.from(onlineBlocks)
                      .scan(new ArrayList<>(), (arr, block) -> listAddLimited(arr, block, 8)),
                  (latestImported, latestOnline) -> {
                    HashSet<?> s1 = new HashSet<>(latestImported);
                    HashSet<?> s2 = new HashSet<>(latestOnline);
                    s1.retainAll(s2);
                    return s1.isEmpty() ? SyncMode.Long : SyncMode.Short;
                  })
              .distinctUntilChanged()
              .onErrorContinue((t, o) -> logger.error("Unexpected error: ", t));
    }

    private <A> ArrayList<A> listAddLimited(ArrayList<A> list, A elem, int maxSize) {
      ArrayList<A> ret =
          new ArrayList<>(list.subList(max(0, list.size() + 1 - maxSize), list.size()));
      ret.add(elem);
      return ret;
    }

    public Publisher<SyncMode> getSyncModeStream() {
      return syncModeStream;
    }
  }
}
