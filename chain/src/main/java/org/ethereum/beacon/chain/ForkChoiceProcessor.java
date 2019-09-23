package org.ethereum.beacon.chain;

import org.reactivestreams.Publisher;

public interface ForkChoiceProcessor {
  Publisher<BeaconChainHead> getChainHeads();
}
