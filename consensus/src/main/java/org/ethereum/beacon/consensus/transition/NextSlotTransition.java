package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.SlotNumber;

public class NextSlotTransition implements StateTransition<BeaconStateEx> {
  private final ChainSpec spec;

  public NextSlotTransition(ChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconBlock block, BeaconStateEx stateEx) {
    SpecHelpers specHelpers = new SpecHelpers(spec);

    MutableBeaconState state = stateEx.getCanonicalState().createMutableCopy();

    // state.slot += 1
    SlotNumber newSlot = state.getSlot().increment();
    state.setSlot(newSlot);

    //  Set state.latest_block_roots[(state.slot - 1) % LATEST_BLOCK_ROOTS_LENGTH] = previous_block_root.
    state.getLatestBlockRoots().set(
        state.getSlot().decrement().modulo(spec.getLatestBlockRootsLength()),
        stateEx.getLatestChainBlockHash());

    // If state.slot % LATEST_BLOCK_ROOTS_LENGTH == 0
    // append merkle_root(state.latest_block_roots) to state.batched_block_roots
    if (state.getSlot().modulo(spec.getLatestBlockRootsLength()).getIntValue() == 0) {
      state.getBatchedBlockRoots().add(specHelpers.merkle_root(state.getLatestBlockRoots()));
    }

    return new BeaconStateEx(state.createImmutable(), stateEx.getLatestChainBlockHash());
  }
}
