package org.ethereum.beacon.consensus.spec;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;

import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * State transition.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#beacon-chain-state-transition-function">Beacon
 *     chain state transition function</a> in the spec.
 */
public interface SpecStateTransition extends EpochProcessing, BlockProcessing {

  /*
    def process_slot(state: BeaconState) -> None:
      # Cache state root
      previous_state_root = hash_tree_root(state)
      state.latest_state_roots[state.slot % SLOTS_PER_HISTORICAL_ROOT] = previous_state_root

      # Cache latest block header state root
      if state.latest_block_header.state_root == ZERO_HASH:
          state.latest_block_header.state_root = previous_state_root

      # Cache block root
      previous_block_root = signing_root(state.latest_block_header)
      state.latest_block_roots[state.slot % SLOTS_PER_HISTORICAL_ROOT] = previous_block_root
   */
  default void process_slot(MutableBeaconState state) {
    // Cache state root
    Hash32 previous_state_root = hash_tree_root(state);
    state.getStateRoots().update(
        state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
        root -> previous_state_root);

    // Cache latest block header state root
    if (state.getLatestBlockHeader().getStateRoot().equals(Hash32.ZERO)) {
      state.setLatestBlockHeader(new BeaconBlockHeader(
          state.getLatestBlockHeader().getSlot(),
          state.getLatestBlockHeader().getParentRoot(),
          previous_state_root,
          state.getLatestBlockHeader().getBodyRoot()
      ));
    }

    // Cache block root
    Hash32 previous_block_root = hash_tree_root(state.getLatestBlockHeader());
    state.getBlockRoots().update(
        state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
        root -> previous_block_root);
  }

  /*
    def process_slots(state: BeaconState, slot: Slot) -> None:
      assert state.slot <= slot
      while state.slot < slot:
          process_slot(state)
          # Process epoch on the first slot of the next epoch
          if (state.slot + 1) % SLOTS_PER_EPOCH == 0:
              process_epoch(state)
          state.slot += 1
   */
  default void process_slots(MutableBeaconState state, SlotNumber slot) {
    assertTrue(state.getSlot().lessEqual(slot));
    while (state.getSlot().less(slot)) {
      process_slot(state);
      // Process epoch on the first slot of the next epoch
      if (state.getSlot().increment().modulo(getConstants().getSlotsPerEpoch()).equals(SlotNumber.ZERO)) {
        process_epoch(state);
      }
      state.setSlot(state.getSlot().increment());
    }
  }

  /*
    def state_transition(state: BeaconState, signed_block: SignedBeaconBlock, validate_result: bool=True) -> BeaconState:
      # Process slots (including those with no blocks) since block
      process_slots(state, signed_block.message.slot)
      # Verify signature
      if validate_result:
          assert verify_block_signature(state, signed_block)
      # Process block
      process_block(state, signed_block.message)
      if validate_result:
          assert signed_block.message.state_root == hash_tree_root(state)  # Validate state root
      # Return post-state
      return state
   */
  default MutableBeaconState state_transition(MutableBeaconState state, SignedBeaconBlock signed_block, boolean validate_result) {
    // Process slots (including those with no blocks) since block
    process_slots(state, signed_block.getMessage().getSlot());
    // Verify signature
    if (validate_result) {
      assertTrue(verify_block_signature(state, signed_block));
    }
    // Process block
    process_block(state, signed_block.getMessage());
    // Validate state root
    if (validate_result) {
      assertTrue(signed_block.getMessage().getStateRoot().equals(hash_tree_root(state)));
    }
    // Return post-state
    return state;
  }

  /*
    def verify_block_signature(state: BeaconState, signed_block: SignedBeaconBlock) -> bool:
      proposer = state.validators[get_beacon_proposer_index(state)]
      domain = get_domain(state, DOMAIN_BEACON_PROPOSER)
      return bls_verify(proposer.pubkey, hash_tree_root(signed_block.message), signed_block.signature, domain)
   */
  default boolean verify_block_signature(BeaconState state, SignedBeaconBlock signed_block) {
    ValidatorRecord proposer = state.getValidators().get(get_beacon_proposer_index(state));
    UInt64 domain = get_domain(state, BEACON_PROPOSER);
    return bls_verify(
        proposer.getPubKey(),
        hash_tree_root(signed_block.getMessage()),
        signed_block.getSignature(),
        domain);
  }
}
