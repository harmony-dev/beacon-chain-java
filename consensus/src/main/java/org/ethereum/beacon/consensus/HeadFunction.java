package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;
import java.util.Optional;

/** Head function updates head */
public interface HeadFunction {

  /**
   * Updates head on chain and returns new head if it changes
   *
   * @return new head, if it was changed
   */
  Optional<BeaconBlock> update();
}
