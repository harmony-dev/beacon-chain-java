package org.ethereum.beacon.chain;

import org.reactivestreams.Publisher;

public interface BeaconChain {

  Publisher<BeaconTupleDetails> getBlockStatesStream();

  /**
   * Returns the most recent processed tuple.
   *
   * <p><strong>Note:</strong> it's not necessary a chain head.
   *
   * @return recently processed tuple.
   */
  BeaconTuple getRecentlyProcessed();

  void init();
}
