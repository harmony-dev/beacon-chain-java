package org.ethereum.beacon.wire.sync;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireInvalidConsensusDataException;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class SyncManager {
  private static final Logger logger = LogManager.getLogger(SyncManager.class);

  MutableBeaconChain chain;
  BeaconChainStorage storage;
  BeaconChainSpec spec;

  WireApiSync syncApi;
  SyncQueue syncQueue;

  int maxConcurrentBlockRequests = 32;

  public SyncManager(MutableBeaconChain chain,
      BeaconChainStorage storage, BeaconChainSpec spec, WireApiSync syncApi,
      SyncQueue syncQueue, int maxConcurrentBlockRequests) {
    this.chain = chain;
    this.storage = storage;
    this.spec = spec;
    this.syncApi = syncApi;
    this.syncQueue = syncQueue;
    this.maxConcurrentBlockRequests = maxConcurrentBlockRequests;
  }

  public void start() {

    Hash32 genesisBlockRoot =
        storage.getBlockStorage().getSlotBlocks(spec.getConstants().getGenesisSlot()).get(0);

    Flux<Hash32> finalizedBlockRootStream = Flux
        .from(chain.getBlockStatesStream())
        .map(bs -> bs.getFinalState().getFinalizedRoot())
        .distinct()
        .map(br -> Hash32.ZERO.equals(br) ? genesisBlockRoot : br);

    Flux<BeaconBlock> finalizedBlockStream =
        finalizedBlockRootStream.map(
            root ->
                storage.getBlockStorage().get(root).orElseThrow(() -> new IllegalStateException()));

    syncQueue.subscribeToFinalBlocks(finalizedBlockStream);

    Flux.from(syncQueue.getBlocksStream()).subscribe(block -> {
      if (!chain.insert(block.get())) {
        block.feedbackError(new WireInvalidConsensusDataException("Couldn't insert block: " + block.get()));
      } else {
        block.feedbackSuccess();
      }
    });

    Flux<Feedback<List<BeaconBlock>>> wireBlocksStream = Flux
        .from(syncQueue.getBlockRequestsStream())
        .map(req -> new BlockHeadersRequestMessage(
            req.getStartRoot().orElse(BlockHeadersRequestMessage.NULL_START_ROOT),
            req.getStartSlot().orElse(BlockHeadersRequestMessage.NULL_START_SLOT),
            req.getMaxCount(),
            req.getStep()))
        .flatMap(req -> Mono.fromFuture(syncApi.requestBlocks(req, spec.getObjectHasher())),
            maxConcurrentBlockRequests)
        .onErrorContinue((t, o) -> logger.info("SyncApi exception: " + t + ", " + o, t));

    syncQueue.subscribeToNewBlocks(wireBlocksStream);
  }
}
