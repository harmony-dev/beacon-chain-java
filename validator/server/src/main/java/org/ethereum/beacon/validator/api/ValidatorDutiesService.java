package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Validator tasks service: searching for tasks, proposing and attesting */
public class ValidatorDutiesService {
  private final BeaconChainSpec spec;
  private final BeaconChainAttester attester;
  private final BeaconChainProposer proposer;

  public ValidatorDutiesService(
      BeaconChainSpec spec,
      BlockTransition<BeaconStateEx> perBlockTransition,
      DepositContract depositContract) {
    this.spec = spec;
    this.attester = new BeaconChainAttesterImpl(spec);
    this.proposer = new BeaconChainProposerImpl(spec, perBlockTransition, depositContract);
  }

  public BeaconBlock prepareBlock(
      SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableBeaconState) {
    return proposer.propose(observableBeaconState, randaoReveal);
  }

  public Attestation prepareAttestation(
      SlotNumber slot,
      ValidatorIndex validatorIndex,
      CommitteeIndex committeeIndex,
      ObservableBeaconState observableBeaconState) {
    return attester.attest(
        validatorIndex,
        committeeIndex,
        observableBeaconState.getLatestSlotState(),
        observableBeaconState.getHead());
  }

  /**
   * Produces map of validator duties (attesting and proposing) for provided epoch with provided
   * state
   *
   * @param epoch Epoch
   * @param state Beacon state
   * @return Map: Slot: Pair with [Proposer, Attesters]
   */
  public Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> getValidatorDuties(
      BeaconState state, EpochNumber epoch) {
    SlotNumber startSlot = spec.compute_start_slot_of_epoch(epoch);
    Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> epochCommitees = new HashMap<>();
    for (SlotNumber slot = startSlot;
        slot.less(startSlot.plus(spec.getConstants().getSlotsPerEpoch()));
        slot = slot.increment()) {
      List<ShardCommittee> ret = new ArrayList<>();
      ValidatorIndex proposerIndex = null;
      for (UInt64 index :
          UInt64s.iterate(
              UInt64.ZERO,
              spec.get_committee_count_at_slot(state, slot))) {
        List<ValidatorIndex> committee = spec.get_beacon_committee(state, slot, new CommitteeIndex(index));
        if (ret.isEmpty()) { // first committee
          proposerIndex = spec.get_beacon_proposer_index(state);
        }
        ret.add(new ShardCommittee(committee, new CommitteeIndex(index)));
      }
      epochCommitees.put(slot, Pair.with(proposerIndex, ret));
    }

    return epochCommitees;
  }

  /**
   * Returns validator duties for provided validator
   *
   * @param validatorIndex Validator index
   * @param duties Epoch validator duties
   * @return Block proposal slot, Attester committee index, Attester slot
   */
  public Triplet<BigInteger, Integer, BigInteger> findDutyForValidator(
      ValidatorIndex validatorIndex,
      Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> duties) {
    BigInteger blockProposal = null;
    Integer attesterCommitteeIndex = null;
    BigInteger attesterSlot = null;
    boolean attesterFound = false;
    boolean proposerFound = false;
    for (Map.Entry<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> entry :
        duties.entrySet()) {
      if (!proposerFound && entry.getValue().getValue0().equals(validatorIndex)) {
        blockProposal = entry.getKey().toBI();
        proposerFound = true;
      }

      if (!attesterFound) {
        for (ShardCommittee shardCommittee : entry.getValue().getValue1()) {
          if (shardCommittee.getCommittee().contains(validatorIndex)) {
            attesterCommitteeIndex = shardCommittee.getIndex().intValue();
            attesterSlot = entry.getKey().toBI();
            attesterFound = true;
            break;
          }
        }
      }

      if (proposerFound && attesterFound) {
        return Triplet.with(blockProposal, attesterCommitteeIndex, attesterSlot);
      }
    }

    return Triplet.with(blockProposal, attesterCommitteeIndex, attesterSlot);
  }
}
