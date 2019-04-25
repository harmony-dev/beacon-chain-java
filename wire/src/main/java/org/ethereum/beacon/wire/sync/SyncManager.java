package org.ethereum.beacon.wire.sync;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireInvalidConsensusDataException;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class SyncManager {
  private static final Logger logger = LogManager.getLogger(SyncManager.class);

  MutableBeaconChain chain;
  BeaconChainStorage storage;
  WireApiSync syncApi;
  BeaconChainSpec spec;

  int maxConcurrentBlockRequests = 32;

  SyncQueue syncQueue;

  public void start() {

    Flux<Hash32> finalizedBlockRootStream = Flux
        .from(chain.getBlockStatesStream())
        .map(bs -> bs.getFinalState().getFinalizedRoot())
        .distinct();

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
            req.getStartRoot().get(),
            req.getStartSlot().get(),
            req.getMaxCount(),
            req.getStep()))
        .flatMap(req -> Mono.fromFuture(syncApi.requestBlocks(req, spec.getObjectHasher())),
            maxConcurrentBlockRequests)
        .onErrorContinue((t, o) -> logger.info("SyncApi exception: " + t + ", " + o));

    syncQueue.subscribeToNewBlocks(wireBlocksStream);
  }
}
