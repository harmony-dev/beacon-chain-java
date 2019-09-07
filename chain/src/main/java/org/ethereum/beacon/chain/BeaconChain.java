package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.state.Checkpoint;
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

  /**
   * Returns the most recent justified checkpoint.
   *
   * @return a checkpoint.
   */
  Publisher<Checkpoint> getJustifiedCheckpoints();

  /**
   * Returns the most recent finalized checkpoint.
   *
   * <p><b>Note:</b> finalized checkpoints are published by {@link #getJustifiedCheckpoints()}
   * either.
   *
   * @return a checkpoint.
   */
  Publisher<Checkpoint> getFinalizedCheckpoints();

  void init();
}
