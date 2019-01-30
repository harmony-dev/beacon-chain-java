package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;

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

    // state.validator_registry[get_beacon_proposer_index(state, state.slot)].proposer_slots += 1
    state.getValidatorRegistry().update(specHelpers.get_beacon_proposer_index(state, newSlot),
        v -> v.builder().withProposerSlots(v.getProposerSlots().increment()).build());

    // state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] =
    //   state.latest_randao_mixes[(state.slot - 1) % LATEST_RANDAO_MIXES_LENGTH]
    state.getLatestRandaoMixes().set(
        newSlot.modulo(spec.getLatestRandaoMixesLength()),
        state.getLatestRandaoMixes().get(
            newSlot.decrement().modulo(spec.getLatestRandaoMixesLength())));

    //  Set state.latest_block_roots[(state.slot - 1) % LATEST_BLOCK_ROOTS_LENGTH] = previous_block_root.
    state.getLatestBlockRoots().set(
        state.getSlot().decrement().modulo(spec.getLatestBlockRootsLength()).getIntValue(),
        stateEx.getLatestChainBlockHash());

    // If state.slot % LATEST_BLOCK_ROOTS_LENGTH == 0
    // append merkle_root(state.latest_block_roots) to state.batched_block_roots
    if (state.getSlot().modulo(spec.getLatestBlockRootsLength()).getIntValue() == 0) {
      state.getBatchedBlockRoots().add(specHelpers.merkle_root(state.getLatestBlockRoots()));
    }

    return new BeaconStateEx(state.createImmutable(), stateEx.getLatestChainBlockHash());
  }
}
