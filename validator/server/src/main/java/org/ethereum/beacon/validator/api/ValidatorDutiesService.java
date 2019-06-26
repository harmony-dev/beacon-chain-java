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
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.validator.BeaconAttesterSpec;
import org.ethereum.beacon.validator.BeaconProposerSpec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidatorDutiesService {
  private final BeaconChainSpec spec;
  private final BeaconAttesterSpec attesterSpec;
  private final BeaconProposerSpec proposerSpec;
  private final BlockTransition<BeaconStateEx> perBlockTransition;
  private final DepositContract depositContract;

  public ValidatorDutiesService(BeaconChainSpec spec, BlockTransition<BeaconStateEx> perBlockTransition, DepositContract depositContract) {
    this.spec = spec;
    this.attesterSpec = new BeaconAttesterSpec(spec);
    this.proposerSpec = new BeaconProposerSpec(spec);
    this.perBlockTransition = perBlockTransition;
    this.depositContract = depositContract;
  }

  public BeaconBlock.Builder prepareBlock(SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableBeaconState) {
    return proposerSpec.prepareBuilder(perBlockTransition, depositContract, slot, randaoReveal, observableBeaconState);
  }

  public Attestation prepareAttestation(ValidatorIndex validatorIndex, ShardNumber shard, ObservableBeaconState observableBeaconState, SlotNumber slot) {
    return attesterSpec.prepareAttestation(validatorIndex, shard, observableBeaconState, slot);
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
    SlotNumber epochStart = spec.get_epoch_start_slot(epoch);
    UInt64 committeesPerSlot =
        spec.get_epoch_committee_count(state, epoch)
            .dividedBy(spec.getConstants().getSlotsPerEpoch());
    Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> epochCommitees = new HashMap<>();
    for (SlotNumber slotOffset = SlotNumber.ZERO;
        slotOffset.less(spec.getConstants().getSlotsPerEpoch());
        slotOffset = slotOffset.increment()) {
      List<ShardCommittee> ret = new ArrayList<>();
      ValidatorIndex proposerIndex = null;
      for (UInt64 offset :
          UInt64s.iterate(
              committeesPerSlot.times(slotOffset),
              committeesPerSlot.times(slotOffset.increment()))) {
        ShardNumber shard =
            spec.get_epoch_start_shard(state, epoch)
                .plusModulo(offset, spec.getConstants().getShardCount());
        List<ValidatorIndex> committee = spec.get_crosslink_committee(state, epoch, shard);
        if (ret.isEmpty()) { // first committee
          proposerIndex = spec.get_beacon_proposer_index_for_committee(state, epoch, committee);
        }
        ret.add(new ShardCommittee(committee, shard));
      }
      epochCommitees.put(slotOffset.plus(epochStart), Pair.with(proposerIndex, ret));
    }

    return epochCommitees;
  }

  /**
   * Returns validator duties for provided validator
   *
   * @param validatorIndex Validator index
   * @param duties Epoch validator duties
   * @return Block proposal slot, Attester shard, Attester slot
   */
  public Triplet<BigInteger, Integer, BigInteger> findDutyForValidator(
      ValidatorIndex validatorIndex,
      Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> duties) {
    BigInteger blockProposal = null;
    Integer attesterShard = null;
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
            attesterShard = shardCommittee.getShard().intValue();
            attesterSlot = entry.getKey().toBI();
            attesterFound = true;
            break;
          }
        }
      }

      if (proposerFound && attesterFound) {
        return Triplet.with(blockProposal, attesterShard, attesterSlot);
      }
    }

    return Triplet.with(blockProposal, attesterShard, attesterSlot);
  }
}
