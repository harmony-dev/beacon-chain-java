package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.reactivestreams.Publisher;

/**
 * Handles own proposed blocks, i.e. imports them to the chain and on success
 * forwards them to the outbound stream for further broadcasting
 */
public interface ProposedBlockProcessor {

  void newBlockProposed(BeaconBlock newBlcok);

  Publisher<BeaconBlock> processedBlocksStream();

}
