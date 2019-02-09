package org.ethereum.beacon.consensus;

/**
 * A pure state transition interface.
 *
 * <p>Used as an interface to per-slot and per-epoch transitions that accepts only a source state.
 *
 * @param <State> a state type.
 * @see BlockTransition
 */
public interface StateTransition<State> {

  /**
   * Applies a transition function.
   *
   * @param source a source state.
   * @return a source state modified by a transition function.
   */
  State apply(State source);
}
