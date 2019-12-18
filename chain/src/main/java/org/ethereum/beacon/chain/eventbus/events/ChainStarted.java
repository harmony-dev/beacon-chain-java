package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.consensus.ChainStart;

public class ChainStarted implements Event<ChainStart> {

  public static ChainStarted wrap(ChainStart chainStart) {
    return new ChainStarted(chainStart);
  }

  private final ChainStart chainStart;

  public ChainStarted(ChainStart chainStart) {
    this.chainStart = chainStart;
  }

  @Override
  public ChainStart getValue() {
    return chainStart;
  }
}
