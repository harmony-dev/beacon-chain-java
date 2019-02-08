package org.ethereum.beacon.consensus;

public interface StateTransition<State> {

  State apply(State state);
}
