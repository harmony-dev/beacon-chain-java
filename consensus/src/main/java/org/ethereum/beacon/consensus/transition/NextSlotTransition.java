package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
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
    UInt64 newSlot = state.getSlot().increment();
    state.withSlot(newSlot);

    // state.validator_registry[get_beacon_proposer_index(state, state.slot)].proposer_slots += 1
    List<ValidatorRecord> newValidatorRegistry = new ArrayList<>(state.getValidatorRegistry());
    UInt24 idx = specHelpers.get_beacon_proposer_index(state, newSlot);
    ValidatorRecord validatorRecord = newValidatorRegistry.get(idx.getValue());
    ValidatorRecord newValidatorRecord = ValidatorRecord.Builder
        .fromRecord(validatorRecord)
        .withProposerSlots(validatorRecord.getProposerSlots().increment())
        .build();
    newValidatorRegistry.set(idx.getValue(), newValidatorRecord);
    state.withValidatorRegistry(newValidatorRegistry);

    // state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] =
    //   state.latest_randao_mixes[(state.slot - 1) % LATEST_RANDAO_MIXES_LENGTH]
    List<Hash32> newRandaoMixes = new ArrayList<>(state.getLatestRandaoMixes());
    newRandaoMixes.set(safeInt(newSlot.modulo(spec.getLatestRandaoMixesLength())),
        newRandaoMixes.get(safeInt(newSlot.decrement().modulo(spec.getLatestRandaoMixesLength()))));
    state.withLatestRandaoMixes(newRandaoMixes);

    //  Set state.latest_block_roots[(state.slot - 1) % LATEST_BLOCK_ROOTS_LENGTH] = previous_block_root.
    ArrayList<Hash32> newLatestBlockRoots = new ArrayList<>(state.getLatestBlockRoots());
    newLatestBlockRoots.set(state.getSlot().decrement()
        .modulo(spec.getLatestBlockRootsLength()).getIntValue(),
        stateEx.getLatestChainBlockHash());
    state.setLatestBlockRoots(newLatestBlockRoots);

    // If state.slot % LATEST_BLOCK_ROOTS_LENGTH == 0
    // append merkle_root(state.latest_block_roots) to state.batched_block_roots
    if (state.getSlot().modulo(spec.getLatestBlockRootsLength()).getIntValue() == 0) {
      ArrayList<Hash32> newBatchedBlocks = new ArrayList<>(state.getBatchedBlockRoots());
      newBatchedBlocks.add(specHelpers.merkle_root(state.getLatestBlockRoots()));
      state.setBatchedBlockRoots(newLatestBlockRoots);
    }

    return new BeaconStateEx(state.validate(), stateEx.getLatestChainBlockHash());
  }
}
