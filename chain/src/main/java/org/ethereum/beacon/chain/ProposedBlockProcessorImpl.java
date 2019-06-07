package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.MutableBeaconChain.ImportResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class ProposedBlockProcessorImpl implements ProposedBlockProcessor {

  private final SimpleProcessor<BeaconBlock> blocksStream;

  private final MutableBeaconChain beaconChain;

  public ProposedBlockProcessorImpl(MutableBeaconChain beaconChain, Schedulers schedulers) {
    this.beaconChain = beaconChain;
    blocksStream = new SimpleProcessor<>(schedulers.events(), "ProposedBlocksProcessor.blocks");
  }

  @Override
  public void newBlockProposed(BeaconBlock newBlock) {
    ImportResult result = beaconChain.insert(newBlock);
    if (result == ImportResult.OK) {
      blocksStream.onNext(newBlock);
    }
  }

  @Override
  public Publisher<BeaconBlock> processedBlocksStream() {
    return blocksStream;
  }
}
