package org.ethereum.beacon.wire.sync;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class SyncManager {
  MutableBeaconChain chain;
  BeaconChainStorage storage;
  WireApiSync syncApi;
  BeaconChainSpec spec;

  int maxBlockRequest;

  public void start() {
    Flux<EpochNumber> finalizedEpochStream = Flux.from(chain.getBlockStatesStream())
        .map(bs -> bs.getFinalState().getFinalizedEpoch())
        .distinct();
    Flux<SlotNumber> finalizedSlotStream = finalizedEpochStream.
        map(epoch -> spec.get_epoch_start_slot(epoch)
            .plus(spec.getConstants().getSlotsPerEpoch()))
        .distinct();
    Flux<Hash32> finalizedBlockRootStream = Flux
        .from(chain.getBlockStatesStream())
        .map(bs -> bs.getFinalState().getFinalizedRoot())
        .distinct();

    Hash32 finalBlock = finalizedBlockRootStream.last().block(Duration.ofSeconds(10));

    CompletableFuture<List<BeaconBlock>> blocksFut = syncApi.requestBlocks(
        new BlockHeadersRequestMessage(
            finalBlock, SlotNumber.ZERO, UInt64.valueOf(maxBlockRequest), UInt64.ZERO),
        spec.getObjectHasher());

    blocksFut.thenAccept(blocks -> blocks.forEach(chain::insert));

  }

}
