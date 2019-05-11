package org.ethereum.beacon.consensus.spec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Fork choice rule.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#beacon-chain-fork-choice-rule">Beacon
 *     chain fork choice rule</a> in the spec.
 */
public interface ForkChoice extends HelperFunction {

  /*
    def lmd_ghost(store: Store, start_state: BeaconState, start_block: BeaconBlock) -> BeaconBlock:
      """
      Execute the LMD-GHOST algorithm to find the head ``BeaconBlock``.
      """
      validators = start_state.validator_registry
      active_validators = [
          validators[i]
          for i in get_active_validator_indices(validators, start_state.slot)
      ]
      attestation_targets = [
          get_latest_attestation_target(store, validator)
          for validator in active_validators
      ]

      def get_vote_count(block: BeaconBlock) -> int:
          return len([
              target
              for target in attestation_targets
              if get_ancestor(store, target, block.slot) == block
          ])

      head = start_block
      while 1:
          children = get_children(store, head)
          if len(children) == 0:
              return head
          head = max(children, key=get_vote_count)
   */
  // FIXME should be epoch parameter get_active_validator_indices(validators, start_state.slot)
  default BeaconBlock lmd_ghost(
      BeaconBlock startBlock,
      BeaconState state,
      Function<Hash32, Optional<BeaconBlock>> getBlock,
      Function<Hash32, List<BeaconBlock>> getChildrenBlocks,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation) {
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state, get_current_epoch(state));

    List<Pair<ValidatorIndex, ValidatorRecord>> active_validators = new ArrayList<>();
    for (ValidatorIndex index : active_validator_indices) {
      active_validators.add(Pair.with(index, state.getValidatorRegistry().get(index)));
    }

    List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets = new ArrayList<>();
    for (Pair<ValidatorIndex, ValidatorRecord> validatorRecord : active_validators) {
      get_latest_attestation_target(validatorRecord.getValue1(), get_latest_attestation, getBlock)
          .ifPresent(block -> attestation_targets.add(Pair.with(validatorRecord.getValue0(), block)));
    }

    BeaconBlock head = startBlock;
    while (true) {
      List<BeaconBlock> children = getChildrenBlocks.apply(signing_root(head));
      if (children.isEmpty()) {
        return head;
      } else {
        head =
            children.stream()
                .max(
                    Comparator.comparing(o -> get_vote_count(state, o, attestation_targets, getBlock),
                        UInt64::compareTo))
                .get();
      }
    }
  }

  /**
   * Let get_latest_attestation_target(store, validator) be the target block in the attestation
   * get_latest_attestation(store, validator).
   *
   * @param get_latest_attestation Let get_latest_attestation(store, validator) be the attestation
   *     with the highest slot number in store from validator. If several such attestations exist,
   *     use the one the validator v observed first.
   */
  default Optional<BeaconBlock> get_latest_attestation_target(
      ValidatorRecord validatorRecord,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {
    Optional<Attestation> latest = get_latest_attestation.apply(validatorRecord);
    return latest.flatMap(at -> getBlock.apply(at.getData().getSourceRoot()));
  }

  /*
    def get_vote_count(block: BeaconBlock) -> int:
      return sum(
          get_effective_balance(start_state.validator_balances[validator_index]) // EFFECTIVE_BALANCE_INCREMENT
          for validator_index, target in attestation_targets
          if get_ancestor(store, target, block.slot) == block
      )
   */
  default UInt64 get_vote_count(
      BeaconState startState,
      BeaconBlock block,
      List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {

    return attestation_targets.stream().filter(
        target -> get_ancestor(target.getValue1(), block.getSlot(), getBlock)
            .filter(ancestor -> ancestor.equals(block)).isPresent())
        .map(target -> get_effective_balance(startState, target.getValue0()).dividedBy(getConstants().getEffectiveBalanceIncrement()))
        .reduce(Gwei.ZERO, Gwei::plus);
  }

  /*
    def get_ancestor(store: Store, block: BeaconBlock, slot: SlotNumber) -> BeaconBlock:
      """
      Get the ancestor of ``block`` with slot number ``slot``; return ``None`` if not found.
      """
      if block.slot == slot:
          return block
      elif block.slot < slot:
          return None
      else:
          return get_ancestor(store, store.get_parent(block), slot)
   */
  default Optional<BeaconBlock> get_ancestor(
      BeaconBlock block, SlotNumber slot, Function<Hash32, Optional<BeaconBlock>> getBlock) {
    if (block.getSlot().equals(slot)) {
      return Optional.of(block);
    } else if (block.getSlot().less(slot)) {
      return Optional.empty();
    } else {
      return getBlock
          .apply(block.getPreviousBlockRoot())
          .flatMap(parent -> get_ancestor(parent, slot, getBlock));
    }
  }
}
