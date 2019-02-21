package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class ProposedBlockProcessorImpl implements ProposedBlockProcessor {

  private final DirectProcessor<BeaconBlock> blocksSink = DirectProcessor.create();
  private final Publisher<BeaconBlock> blocksStream;

  private final MutableBeaconChain beaconChain;

  public ProposedBlockProcessorImpl(MutableBeaconChain beaconChain, Schedulers schedulers) {
    this.beaconChain = beaconChain;
    blocksStream = Flux.from(blocksSink)
            .publishOn(schedulers.reactorEvents())
            .onBackpressureError()
            .name("ProposedBlocksProcessor.blocks");
  }

  @Override
  public void newBlockProposed(BeaconBlock newBlcok) {
    boolean result = beaconChain.insert(newBlcok);
    if (result) {
      blocksSink.onNext(newBlcok);
    }
  }

  @Override
  public Publisher<BeaconBlock> processedBlocksStream() {
    return blocksStream;
  }
}
