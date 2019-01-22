package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The beacon chain fork choice rule is a hybrid that combines justification and finality with
 * Latest Message Driven (LMD) Greediest Heaviest Observed SubTree (GHOST). For more info check <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beacon-chain-fork-choice-rule">Beacon
 * chain fork choice rule</a>
 */
public class GhostHeadFunction implements HeadFunction {

  private final BeaconBlockStorage blockStorage;
  private final BeaconTupleStorage tupleStorage;
  private final SpecHelpers specHelpers;
  private final int CHILDREN_SEARCH_LIMIT = Integer.MAX_VALUE;
  private final Map<Bytes48, Attestation> attestationCache = new HashMap<>();
  private final Map<UInt64, Set<Bytes48>> validatorSlotCache = new HashMap<>();
  private BeaconState state;

  public GhostHeadFunction(
      BeaconChain beaconChain, BeaconChainStorage chainStorage, SpecHelpers specHelpers) {
    this.tupleStorage = chainStorage.getBeaconTupleStorage();
    this.blockStorage = chainStorage.getBeaconBlockStorage();
    this.specHelpers = specHelpers;
    this.state = tupleStorage.getCanonicalHead().getState();
    Flux.from(beaconChain.getBlockStatesStream())
        .doOnNext(beaconTuple -> state = beaconTuple.getState())
        .subscribe();
  }

  @Override
  public Optional<BeaconBlock> update() {
    Hash32 curHead = blockStorage.getCanonicalHead();
    UInt64 lastJustifiedSlot = state.getJustifiedSlot();
    Optional<BeaconTuple> justifiedTuple =
        blockStorage
            .getSlotCanonicalBlock(lastJustifiedSlot.getValue())
            .map(tupleStorage::get)
            .orElseThrow(() -> new RuntimeException("No justified head found"));
    BeaconBlock newHead =
        justifiedTuple
            .map(this::lmd_ghost)
            .orElseThrow(() -> new RuntimeException("No justified head found"));
    if (!newHead.getHash().equals(curHead)) {
      blockStorage.reorgTo(newHead.getHash());
      return Optional.of(newHead);
    } else {
      return Optional.empty();
    }
  }

  // def lmd_ghost(store, start):
  //    validators = start.state.validator_registry
  //    active_validators = [validators[i] for i in
  //                         get_active_validator_indices(validators, start.state.slot)]
  //    attestation_targets = [get_latest_attestation_target(store, validator)
  //                           for validator in active_validators]
  //    def get_vote_count(block):
  //        return len([target for target in attestation_targets if
  //                    get_ancestor(store, target, block.slot) == block])
  //
  //    head = start
  //    while 1:
  //        children = get_children(head)
  //        if len(children) == 0:
  //            return head
  //        head = max(children, key=get_vote_count)
  private BeaconBlock lmd_ghost(BeaconTuple startTuple) {
    List<ValidatorRecord> validators = startTuple.getState().getValidatorRegistry();
    List<UInt24> active_validator_indices =
        specHelpers.get_active_validator_indices(validators, startTuple.getState().getSlot());

    List<ValidatorRecord> active_validators = new ArrayList<>();
    for (UInt24 index : active_validator_indices) {
      active_validators.add(validators.get(index.getValue()));
    }

    List<BeaconBlock> attestation_targets = new ArrayList<>();
    for (ValidatorRecord validatorRecord : active_validators) {
      attestation_targets.add(get_latest_attestation_target(validatorRecord));
    }

    BeaconBlock head = startTuple.getBlock();
    while (true) {
      List<BeaconBlock> children = blockStorage.getChildren(head.getHash(), CHILDREN_SEARCH_LIMIT);
      if (children.isEmpty()) {
        return head;
      } else {
        head =
            children.stream()
                .max(Comparator.comparingInt(o -> get_vote_count(o, attestation_targets)))
                .orElseThrow(() -> new RuntimeException("Couldn't find maximum voted block"));
      }
    }
  }

  /**
   * Let get_latest_attestation_target(store, validator) be the target block in the attestation
   * get_latest_attestation(store, validator).
   */
  private BeaconBlock get_latest_attestation_target(ValidatorRecord validatorRecord) {
    Attestation latest = get_latest_attestation(validatorRecord);
    return blockStorage
        .get(latest.getData().getBeaconBlockRoot())
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Couldn't find attestation target " + latest.getData().getBeaconBlockRoot()));
  }

  /**
   * Let get_latest_attestation(store, validator) be the attestation with the highest slot number in
   * store from validator. If several such attestations exist, use the one the validator v observed
   * first.
   */
  private Attestation get_latest_attestation(ValidatorRecord validatorRecord) {
    return attestationCache.get(validatorRecord.getPubKey());
  }

  /**
   * def get_vote_count(block): return len([target for target in attestation_targets if
   * get_ancestor(store, target, block.slot) == block])
   */
  private int get_vote_count(BeaconBlock block, List<BeaconBlock> attestation_targets) {
    int res = 0;
    for (BeaconBlock target : attestation_targets) {
      if (get_ancestor(target, block.getSlot()).equals(block)) {
        ++res;
      }
    }

    return res;
  }

  /**
   * Let get_ancestor(store, block, slot) be the ancestor of block with slot number slot. The
   * get_ancestor function can be defined recursively as def get_ancestor(store, block, slot):
   * return block if block.slot == slot else get_ancestor(store, store.get_parent(block), slot).
   */
  private BeaconBlock get_ancestor(BeaconBlock block, UInt64 slot) {
    if (block.getSlot().equals(slot)) {
      return block;
    } else {
      return blockStorage
          .get(block.getParentRoot())
          .map(parent -> get_ancestor(parent, slot))
          .orElseThrow(() -> new RuntimeException("Couldn't find ancestor of block " + block));
    }
  }

  /**
   * This should be implemented via some kind of subscription we should be subscribed to all
   * verified attestations, even those not included in blocks yet
   */
  public void addAttestation(Attestation attestation) {
    List<UInt24> participants =
        specHelpers.get_attestation_participants(
            state, attestation.getData(), attestation.getParticipationBitfield());

    List<Bytes48> pubKeys = specHelpers.mapIndicesToPubKeys(state, participants);

    for (Bytes48 pubKey : pubKeys) {
      if (attestationCache.containsKey(pubKey)) {
        Attestation oldAttestation = attestationCache.get(pubKey);
        if (attestation.getData().getSlot().compareTo(oldAttestation.getData().getSlot()) > 0) {
          attestationCache.put(pubKey, attestation);
          validatorSlotCache.get(oldAttestation.getData().getSlot()).remove(pubKey);
          addToSlotCache(attestation.getData().getSlot(), pubKey);
        } else {
          // XXX: If several such attestations exist, use the one the validator v observed first
          // so no need to swap it
        }
      } else {
        attestationCache.put(pubKey, attestation);
        addToSlotCache(attestation.getData().getSlot(), pubKey);
      }
    }
  }

  private void addToSlotCache(UInt64 slot, Bytes48 pubKey) {
    if (validatorSlotCache.containsKey(slot)) {
      validatorSlotCache.get(slot).add(pubKey);
    } else {
      Set<Bytes48> pubKeysSet = new HashSet<>();
      pubKeysSet.add(pubKey);
      validatorSlotCache.put(slot, pubKeysSet);
    }
  }

  /** Purges all entries for slot and before */
  public void purgeAttestations(UInt64 slot) {
    for (Map.Entry<UInt64, Set<Bytes48>> entry : validatorSlotCache.entrySet()) {
      if (entry.getKey().compareTo(slot) <= 0) {
        entry.getValue().forEach(attestationCache::remove);
        validatorSlotCache.remove(entry.getKey());
      }
    }
  }
}
