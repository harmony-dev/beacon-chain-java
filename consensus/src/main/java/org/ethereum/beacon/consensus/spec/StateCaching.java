package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.MutableBeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * State caching.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#state-caching">State
 *     caching</a> in the spec.
 */
public interface StateCaching extends HelperFunction {

  /*
    At every slot > GENESIS_SLOT run the following function
    Note: this function mutates beacon state
   */
  default void cache_state(MutableBeaconState state) {
    Hash32 previousSlotStateRoot = hash_tree_root(state);

    // store the previous slot's post state transition root
    state.getLatestStateRoots()
        .set(state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()), previousSlotStateRoot);

    // cache state root in stored latest_block_header if empty
    if (state.getLatestBlockHeader().getStateRoot().equals(Hash32.ZERO)) {
      state.setLatestBlockHeader(state.getLatestBlockHeader().withStateRoot(previousSlotStateRoot));
    }

    // store latest known block for previous slot
    state.getLatestBlockRoots()
        .set(
            state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
            signed_root(state.getLatestBlockHeader()));
  }
}
