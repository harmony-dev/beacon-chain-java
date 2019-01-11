package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.SpecHelpers;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;

import static org.ethereum.beacon.core.SpecHelpers.safeInt;

public class NextSlotTransition implements StateTransition<BeaconState> {
  private final ChainSpec spec;

  public NextSlotTransition(ChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconState apply(BeaconBlock block, BeaconState state) {
    SpecHelpers specHelpers = new SpecHelpers(spec);

    BeaconState.Builder builder = BeaconState.Builder.fromState(state);

    // state.slot += 1
    UInt64 newSlot = state.getSlot().increment();
    builder.withSlot(newSlot);

    // state.validator_registry[get_beacon_proposer_index(state, state.slot)].randao_layers += 1
    List<ValidatorRecord> newValidatorRegistry = state.extractValidatorRegistry();
    UInt24 idx = specHelpers.get_beacon_proposer_index(state, newSlot);
    ValidatorRecord validatorRecord = newValidatorRegistry.get(idx.getValue());
    ValidatorRecord newValidatorRecord = ValidatorRecord.Builder
        .fromRecord(validatorRecord)
        .withRandaoLayers(validatorRecord.getRandaoLayers().increment())
        .build();
    newValidatorRegistry.set(idx.getValue(), newValidatorRecord);
    builder.withValidatorRegistry(newValidatorRegistry);

    // state.latest_randao_mixes[state.slot % LATEST_RANDAO_MIXES_LENGTH] =
    //   state.latest_randao_mixes[(state.slot - 1) % LATEST_RANDAO_MIXES_LENGTH]
    List<Hash32> newRandaoMixes = state.extractLatestRandaoMixes();
    newRandaoMixes.set(safeInt(newSlot.modulo(spec.getLatestRandaoMixesLength())),
        newRandaoMixes.get(safeInt(newSlot.decrement().modulo(spec.getLatestRandaoMixesLength()))));
    builder.withLatestRandaoMixes(newRandaoMixes);

    return builder.build();
  }
}
