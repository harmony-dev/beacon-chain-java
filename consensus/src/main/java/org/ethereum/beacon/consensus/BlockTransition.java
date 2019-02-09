package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;

/**
 * A state transition interface accepting a {@link BeaconBlock} as input data.
 *
 * <p>Used as an interface to per-block transition which applies an information from given block to
 * a source state.
 *
 * @param <State> a state type.
 * @see StateTransition
 */
public interface BlockTransition<State> {

  /**
   * Applies a state transition function to given source using given block as an input.
   *
   * @param source a source state.
   * @param input a beacon block with input data.
   * @return a source state modified by a transition function with a help of block data.
   */
  State apply(State source, BeaconBlock input);
}
