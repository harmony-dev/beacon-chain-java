package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.MutableBeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * State caching.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#state-caching">State
 *     caching</a> in the spec.
 */
public interface StateCaching extends HelperFunction {

  /*
    At every slot > GENESIS_SLOT run the following function
    Note: this function mutates beacon state
   */
  default void cache_state(MutableBeaconState state) {
    /* # Cache latest known state root (for previous slot)
      latest_state_root = hash_tree_root(state)
      state.latest_state_roots[state.slot % SLOTS_PER_HISTORICAL_ROOT] = latest_state_root */
    Hash32 latest_state_root = hash_tree_root(state);
    state.getLatestStateRoots().update(
        state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
        root -> latest_state_root);

    /* # Store latest known state root (for previous slot) in latest_block_header if it is empty
      if state.latest_block_header.state_root == ZERO_HASH:
          state.latest_block_header.state_root = latest_state_root */
    if (state.getLatestBlockHeader().getStateRoot().equals(Hash32.ZERO)) {
      state.setLatestBlockHeader(new BeaconBlockHeader(
          state.getLatestBlockHeader().getSlot(),
          state.getLatestBlockHeader().getPreviousBlockRoot(),
          latest_state_root,
          state.getLatestBlockHeader().getBlockBodyRoot(),
          state.getLatestBlockHeader().getSignature()
      ));
    }

    /*  # Cache latest known block root (for previous slot)
      latest_block_root = signing_root(state.latest_block_header)
      state.latest_block_roots[state.slot % SLOTS_PER_HISTORICAL_ROOT] = latest_block_root */
    Hash32 latest_block_root = signing_root(state.getLatestBlockHeader());
    state.getLatestBlockRoots().update(
        state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
        root -> latest_block_root);
  }
}
