package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.core.BeaconState;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface BeaconChain {

  Publisher<BeaconTuple> getBlockStatesStream();

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
