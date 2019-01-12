package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.Hashable;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconTuple implements Hashable<Hash32> {

  private final BeaconBlock block;
  private final BeaconState state;

  BeaconTuple(BeaconBlock block, BeaconState state) {
    this.block = block;
    this.state = state;
  }

  public static BeaconTuple of(BeaconBlock block, BeaconState state) {
    return new BeaconTuple(block, state);
  }

  public BeaconBlock getBlock() {
    return block;
  }

  public BeaconState getState() {
    return state;
  }

  @Override
  public Hash32 getHash() {
    return block.getHash();
  }
}
