package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fork choice rule.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_fork-choice.md">Beacon
 *     Chain Fork Choice</a> in the spec.
 */
public interface ForkChoice extends HelperFunction, SpecStateTransition {

  /*
    def get_ancestor(store: Store, root: Hash, slot: Slot) -> Hash:
      block = store.blocks[root]
      if block.slot > slot:
          return get_ancestor(store, block.parent_root, slot)
      elif block.slot == slot:
          return root
      else:
          return Bytes32()  # root is older than queried slot: no results.
   */
  default Optional<Hash32> get_ancestor(Store store, Hash32 root, SlotNumber slot) {
    Optional<BeaconBlock> aBlock = store.getBlock(root);
    if (!aBlock.isPresent()) {
      return Optional.empty();
    }

    BeaconBlock block = aBlock.get();
    if (block.getSlot().greater(slot)) {
      return get_ancestor(store, block.getParentRoot(), slot);
    } else if (block.getSlot().equals(slot)) {
      return Optional.of(root);
    } else {
      return Optional.of(Hash32.ZERO);
    }
  }

  /*
    def get_latest_attesting_balance(store: Store, root: Hash) -> Gwei:
      state = store.checkpoint_states[store.justified_checkpoint]
      active_indices = get_active_validator_indices(state, get_current_epoch(state))
      return Gwei(sum(
          state.validators[i].effective_balance for i in active_indices
          if (i in store.latest_messages
              and get_ancestor(store, store.latest_messages[i].root, store.blocks[root].slot) == root)
      ))
   */
  default Gwei get_latest_attesting_balance(Store store, Hash32 root) {
    Optional<BeaconState> state = store.getCheckpointState(store.getJustifiedCheckpoint());
    if (!state.isPresent()) {
      return Gwei.ZERO;
    }

    List<ValidatorIndex> active_indices =
        get_active_validator_indices(state.get(), get_current_epoch(state.get()));

    return active_indices.stream()
        .filter(i -> {
          Optional<LatestMessage> latest_message = store.getLatestMessage(i);
          Optional<BeaconBlock> block = store.getBlock(root);

          if (!latest_message.isPresent() || !block.isPresent()) {
            return false;
          }

          Optional<Hash32> ancestor =
              get_ancestor(store, latest_message.get().root, block.get().getSlot());

          return ancestor.map(hash32 -> hash32.equals(root)).orElse(false);
        })
        .map(i -> state.get().getValidators().get(i).getEffectiveBalance())
        .reduce(Gwei.ZERO, Gwei::plus);
  }

  /*
    def get_head(store: Store) -> Hash:
      # Execute the LMD-GHOST fork choice
      head = store.justified_checkpoint.root
      justified_slot = compute_start_slot_of_epoch(store.justified_checkpoint.epoch)
      while True:
          children = [
              root for root in store.blocks.keys()
              if store.blocks[root].parent_root == head and store.blocks[root].slot > justified_slot
          ]
          if len(children) == 0:
              return head
          # Sort by latest attesting balance with ties broken lexicographically
          head = max(children, key=lambda root: (get_latest_attesting_balance(store, root), root))
   */
  default Hash32 get_head(Store store) {
    // Execute the LMD-GHOST fork choice
    Hash32 head = store.getJustifiedCheckpoint().getRoot();
    while (true) {
      List<Hash32> children = store.getChildren(head);
      if (children.isEmpty()) {
        return head;
      }

      head = children.stream()
          .max(Comparator.comparing(root -> get_latest_attesting_balance(store, root)))
          .get();
    }
  }

  class LatestMessage {
    private final EpochNumber epoch;
    private final Hash32 root;

    public LatestMessage(EpochNumber epoch, Hash32 root) {
      this.epoch = epoch;
      this.root = root;
    }

    public EpochNumber getEpoch() {
      return epoch;
    }

    public Hash32 getRoot() {
      return root;
    }
  }

  interface Store {
    Time getTime();

    void setTime(Time time);

    Time getGenesisTime();

    Checkpoint getJustifiedCheckpoint();

    void setJustifiedCheckpoint(Checkpoint checkpoint);

    Checkpoint getBestJustifiedCheckpoint();

    void setBestJustifiedCheckpoint(Checkpoint checkpoint);

    Checkpoint getFinalizedCheckpoint();

    void setFinalizedCheckpoint(Checkpoint checkpoint);

    Optional<BeaconBlock> getBlock(Hash32 root);

    void setBlock(Hash32 root, BeaconBlock block);

    Optional<BeaconState> getState(Hash32 root);

    void setState(Hash32 root, BeaconState state);

    default Optional<BeaconState> getCheckpointState(Checkpoint checkpoint) {
      Optional<BeaconBlock> block = getBlock(checkpoint.getRoot());
      Optional<Hash32> root = block.flatMap(b -> Optional.of(b.getStateRoot()));
      return root.flatMap(this::getState);
    }

    void setCheckpointState(Checkpoint checkpoint, BeaconState state);

    Optional<LatestMessage> getLatestMessage(ValidatorIndex index);

    void setLatestMessage(ValidatorIndex index, LatestMessage message);

    List<Hash32> getChildren(Hash32 root);
  }

  /*
   def get_current_slot(store: Store) -> Slot:
     return Slot((store.time - store.genesis_time) // SECONDS_PER_SLOT)
  */
  default SlotNumber get_current_slot(Store store) {
    return SlotNumber.castFrom(
            store
                .getTime()
                .minus(store.getGenesisTime())
                .dividedBy(getConstants().getSecondsPerSlot()))
        .plus(getConstants().getGenesisSlot());
  }

  /*
   def should_update_justified_checkpoint(store: Store, new_justified_checkpoint: Checkpoint) -> bool:
     """
     To address the bouncing attack, only update conflicting justified
     checkpoints in the fork choice if in the early slots of the epoch.
     Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

     See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
     """
     if compute_slots_since_epoch_start(get_current_slot(store)) < SAFE_SLOTS_TO_UPDATE_JUSTIFIED:
       return True

     new_justified_block = store.blocks[new_justified_checkpoint.root]
     if new_justified_block.slot <= compute_start_slot_at_epoch(store.justified_checkpoint.epoch):
         return False
     if not (
         get_ancestor(store, new_justified_checkpoint.root, store.blocks[store.justified_checkpoint.root].slot) ==
         store.justified_checkpoint.root
     ):
         return False

     return True
  */
  default boolean should_update_justified_checkpoint(
      Store store, Checkpoint new_justified_checkpoint) {
    if (compute_slots_since_epoch_start(get_current_slot(store))
        .less(getConstants().getSafeSlotsToUpdateJustified())) {
      return true;
    }

    BeaconBlock new_justified_block = store.getBlock(new_justified_checkpoint.getRoot()).get();
    if (new_justified_block
        .getSlot()
        .lessEqual(compute_start_slot_of_epoch(store.getJustifiedCheckpoint().getEpoch()))) {
      return false;
    }

    if (!get_ancestor(
            store,
            new_justified_checkpoint.getRoot(),
            store.getBlock(store.getJustifiedCheckpoint().getRoot()).get().getSlot())
        .equals(store.getJustifiedCheckpoint().getRoot())) {
      return false;
    }
    return true;
  }

  /*
   def on_tick(store: Store, time: uint64) -> None:
     previous_slot = get_current_slot(store)

     # update store time
     store.time = time

     current_slot = get_current_slot(store)
     # Not a new epoch, return
     if not (current_slot > previous_slot and compute_slots_since_epoch_start(current_slot) == 0):
       return
     # Update store.justified_checkpoint if a better checkpoint is known
     if store.best_justified_checkpoint.epoch > store.justified_checkpoint.epoch:
       store.justified_checkpoint = store.best_justified_checkpoint
  */
  default void on_tick(Store store, long time) {
    SlotNumber previous_slot = get_current_slot(store);
    store.setTime(Time.of(time));
    SlotNumber current_slot = get_current_slot(store);
    if (!(current_slot.greater(previous_slot)
        && compute_slots_since_epoch_start(current_slot).equals(SlotNumber.ZERO))) {
      return;
    }
    if (store
        .getBestJustifiedCheckpoint()
        .getEpoch()
        .greater(store.getJustifiedCheckpoint().getEpoch())) {
      store.setJustifiedCheckpoint(store.getBestJustifiedCheckpoint());
    }
  }

  /*
  def on_block(store: Store, block: BeaconBlock) -> None:
    # Make a copy of the state to avoid mutability issues
    assert block.parent_root in store.block_states
    pre_state = store.block_states[block.parent_root].copy()
    # Blocks cannot be in the future. If they are, their consideration must be delayed until the are in the past.
    assert store.time >= pre_state.genesis_time + block.slot * SECONDS_PER_SLOT
    # Add new block to the store
    store.blocks[signing_root(block)] = block
    # Check block is a descendant of the finalized block
    assert (
        get_ancestor(store, signing_root(block), store.blocks[store.finalized_checkpoint.root].slot) ==
        store.finalized_checkpoint.root
    )
    # Check that block is later than the finalized epoch slot
    assert block.slot > compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
    # Check the block is valid and compute the post-state
    state = state_transition(pre_state, block, True)
    # Add new state for this block to the store
    store.block_states[signing_root(block)] = state

    # Update justified checkpoint
    if state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch:
        store.best_justified_checkpoint = state.current_justified_checkpoint
        if should_update_justified_checkpoint(store, state.current_justified_checkpoint):
            store.justified_checkpoint = state.current_justified_checkpoint

    # Update finalized checkpoint
    if state.finalized_checkpoint.epoch > store.finalized_checkpoint.epoch:
        store.finalized_checkpoint = state.finalized_checkpoint
   */
  default void on_block(Store store, BeaconBlock block) {
    // # Make a copy of the state to avoid mutability issues
    assertTrue(store.getBlock(block.getParentRoot()).isPresent());
    MutableBeaconState pre_state = store.getState(block.getParentRoot()).get().createMutableCopy();
    // # Blocks cannot be in the future. If they are, their consideration must be delayed until the
    // are in the past.
    assertTrue(
        store
            .getTime()
            .greater(
                pre_state
                    .getGenesisTime()
                    .plus(getConstants().getSecondsPerSlot().times(block.getSlot()))));

    // # Add new block to the store
    store.setBlock(signing_root(block), block);

    // # Check block is a descendant of the finalized block
    assertTrue(
        get_ancestor(
                store,
                signing_root(block),
                store.getBlock(store.getFinalizedCheckpoint().getRoot()).get().getSlot())
            .get()
            .equals(store.getFinalizedCheckpoint().getRoot()));
    // # Check that block is later than the finalized epoch slot
    assertTrue(
        block
            .getSlot()
            .greater(compute_start_slot_of_epoch(store.getFinalizedCheckpoint().getEpoch())));
    // # Check the block is valid and compute the post-state
    BeaconState state = state_transition(pre_state, block, true).createImmutable();
    // # Add new state for this block to the store
    store.setState(signing_root(block), state);

    // # Update justified checkpoint
    if (state
        .getCurrentJustifiedCheckpoint()
        .getEpoch()
        .greater(store.getJustifiedCheckpoint().getEpoch())) {
      store.setBestJustifiedCheckpoint(state.getCurrentJustifiedCheckpoint());
      if (should_update_justified_checkpoint(store, state.getCurrentJustifiedCheckpoint())) {
        store.setJustifiedCheckpoint(state.getCurrentJustifiedCheckpoint());
      }
    }

    // # Update finalized checkpoint
    if (state
        .getFinalizedCheckpoint()
        .getEpoch()
        .greater(store.getFinalizedCheckpoint().getEpoch())) {
      store.setFinalizedCheckpoint(state.getFinalizedCheckpoint());
    }
  }

  /*
  def on_attestation(store: Store, attestation: Attestation) -> None:
    target = attestation.data.target

    # Attestations must be from the current or previous epoch
    current_epoch = compute_epoch_at_slot(get_current_slot(store))
    # Use GENESIS_EPOCH for previous when genesis to avoid underflow
    previous_epoch = current_epoch - 1 if current_epoch > GENESIS_EPOCH else GENESIS_EPOCH
    assert target.epoch in [current_epoch, previous_epoch]
    # Cannot calculate the current shuffling if have not seen the target
    assert target.root in store.blocks

    # Attestations cannot be from future epochs. If they are, delay consideration until the epoch arrives
    base_state = store.block_states[target.root].copy()
    assert store.time >= base_state.genesis_time + compute_start_slot_at_epoch(target.epoch) * SECONDS_PER_SLOT

    # Store target checkpoint state if not yet seen
    if target not in store.checkpoint_states:
        process_slots(base_state, compute_start_slot_at_epoch(target.epoch))
        store.checkpoint_states[target] = base_state
    target_state = store.checkpoint_states[target]

    # Attestations can only affect the fork choice of subsequent slots.
    # Delay consideration in the fork choice until their slot is in the past.
    assert store.time >= (attestation.data.slot + 1) * SECONDS_PER_SLOT

    # Get state at the `target` to validate attestation and calculate the committees
    indexed_attestation = get_indexed_attestation(target_state, attestation)
    assert is_valid_indexed_attestation(target_state, indexed_attestation)

    # Update latest messages
    for i in indexed_attestation.attesting_indices:
        if i not in store.latest_messages or target.epoch > store.latest_messages[i].epoch:
            store.latest_messages[i] = LatestMessage(epoch=target.epoch, root=attestation.data.beacon_block_root)
   */
  default void on_attestation(Store store, Attestation attestation) {
    // target = attestation.data.target
    Checkpoint target = attestation.getData().getTarget();
    // # Attestations must be from the current or previous epoch
    // current_epoch = compute_epoch_at_slot(get_current_slot(store))
    EpochNumber current_epoch = compute_epoch_of_slot(get_current_slot(store));
    // # Use GENESIS_EPOCH for previous when genesis to avoid underflow
    // previous_epoch = current_epoch - 1 if current_epoch > GENESIS_EPOCH else GENESIS_EPOCH
    EpochNumber previous_epoch =
        current_epoch.greater(getConstants().getGenesisEpoch())
            ? current_epoch.decrement()
            : getConstants().getGenesisEpoch();
    // assert target.epoch in [current_epoch, previous_epoch]
    assertTrue(target.getEpoch().equals(current_epoch) || target.getEpoch().equals(previous_epoch));
    // # Cannot calculate the current shuffling if have not seen the target
    // assert target.root in store.blocks
    assertTrue(store.getBlock(target.getRoot()).isPresent());
    // # Attestations cannot be from future epochs. If they are, delay consideration until the epoch
    // arrives
    // base_state = store.block_states[target.root].copy()
    MutableBeaconState base_state = store.getState(target.getRoot()).get().createMutableCopy();
    // assert store.time >= base_state.genesis_time + compute_start_slot_at_epoch(target.epoch) *
    // SECONDS_PER_SLOT
    assertTrue(
        store
            .getTime()
            .greater(
                base_state
                    .getGenesisTime()
                    .plus(
                        getConstants()
                            .getSecondsPerSlot()
                            .times(compute_start_slot_of_epoch(target.getEpoch())))));
    // # Store target checkpoint state if not yet seen
    // if target not in store.checkpoint_states:
    //   process_slots(base_state, compute_start_slot_at_epoch(target.epoch))
    //   store.checkpoint_states[target] = base_state
    // target_state = store.checkpoint_states[target]
    if (!store.getCheckpointState(target).isPresent()) {
      process_slots(base_state, compute_start_slot_of_epoch(target.getEpoch()));
      store.setCheckpointState(target, base_state);
    }
    BeaconState target_state = store.getCheckpointState(target).get();
    // # Attestations can only affect the fork choice of subsequent slots.
    // # Delay consideration in the fork choice until their slot is in the past.
    // assert store.time >= (attestation.data.slot + 1) * SECONDS_PER_SLOT
    assertTrue(
        store
            .getTime()
            .greater(
                getConstants()
                    .getSecondsPerSlot()
                    .times(attestation.getData().getSlot().increment())));
    // # Get state at the `target` to validate attestation and calculate the committees
    // indexed_attestation = get_indexed_attestation(target_state, attestation)
    // assert is_valid_indexed_attestation(target_state, indexed_attestation)
    IndexedAttestation indexed_attestation = get_indexed_attestation(target_state, attestation);
    assertTrue(is_valid_indexed_attestation(target_state, indexed_attestation));
    // # Update latest messages
    // for i in indexed_attestation.attesting_indices:
    //   if i not in store.latest_messages or target.epoch > store.latest_messages[i].epoch:
    //     store.latest_messages[i] = LatestMessage(epoch=target.epoch,
    // root=attestation.data.beacon_block_root)
    for (ValidatorIndex i : indexed_attestation.getAttestingIndices()) {
      if (!store.getLatestMessage(i).isPresent()
          || target.getEpoch().greater(store.getLatestMessage(i).get().epoch)) {
        store.setLatestMessage(
            i, new LatestMessage(target.getEpoch(), attestation.getData().getBeaconBlockRoot()));
      }
    }
  }
}
