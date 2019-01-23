package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;

/** Head function updates head */
public interface HeadFunction {

  /**
   * Updates actual head on chain and returns it
   *
   * @return head block
   */
  BeaconBlock getHead();
}
