package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.consensus.types.Score;

public class BeaconChainHead {

  private final BeaconTuple tuple;
  private final Score score;

  public BeaconChainHead(BeaconTuple tuple, Score score) {
    this.tuple = tuple;
    this.score = score;
  }

  public static BeaconChainHead of(BeaconTuple tuple, Score score) {
    return new BeaconChainHead(tuple, score);
  }

  public BeaconTuple getTuple() {
    return tuple;
  }

  public Score getScore() {
    return score;
  }

  public BeaconBlock getBlock() {
    return tuple.getBlock();
  }

  public BeaconState getState() {
    return tuple.getState();
  }
}
