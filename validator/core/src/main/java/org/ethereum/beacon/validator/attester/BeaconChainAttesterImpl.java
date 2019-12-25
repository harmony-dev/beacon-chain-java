package org.ethereum.beacon.validator.attester;

import com.google.common.annotations.VisibleForTesting;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.ValidatorService;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.List;

/**
 * An implementation of beacon chain attester.
 *
 * @see BeaconChainAttester
 * @see ValidatorService
 */
public class BeaconChainAttesterImpl implements BeaconChainAttester {

  /** The spec. */
  private BeaconChainSpec spec;

  public BeaconChainAttesterImpl(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public Attestation attest(
      ValidatorIndex validatorIndex, CommitteeIndex index, BeaconState state, BeaconBlock head) {
    Hash32 beaconBlockRoot = spec.hash_tree_root(head);
    EpochNumber targetEpoch = spec.get_current_epoch(state);
    Checkpoint target = getTarget(state, head, targetEpoch);
    Checkpoint source = getSource(state);
    AttestationData data = new AttestationData(state.getSlot(), index, beaconBlockRoot, source, target);

    List<ValidatorIndex> committee = getCommittee(state, index);
    BytesValue participationBitfield = getParticipationBitfield(validatorIndex, committee);
    Bitlist participation =
        Bitlist.of(
            committee.size(),
            participationBitfield,
            spec.getConstants().getMaxValidatorsPerCommittee().intValue());

    return new Attestation(participation, data, BLSSignature.ZERO, spec.getConstants());
  }

  /**
   * Returns a committee at a state slot for a given shard.
   *
   * @param state a state.
   * @param index a committee index.
   * @return a committee.
   */
  @VisibleForTesting
  List<ValidatorIndex> getCommittee(BeaconState state, CommitteeIndex index) {
    return spec.get_beacon_committee(state, state.getSlot(), index);
  }

  @VisibleForTesting
  Checkpoint getTarget(BeaconState state, BeaconBlock head, EpochNumber targetEpoch) {
    SlotNumber epochBoundarySlot =
        spec.compute_start_slot_at_epoch(spec.compute_epoch_at_slot(state.getSlot()));
    if (epochBoundarySlot.equals(state.getSlot())) {
      return new Checkpoint(targetEpoch, spec.hash_tree_root(head));
    } else {
      return new Checkpoint(targetEpoch, spec.get_block_root_at_slot(state, epochBoundarySlot));
    }
  }

  @VisibleForTesting
  Checkpoint getSource(BeaconState state) {
    return state.getCurrentJustifiedCheckpoint();
  }

  /*
   Let aggregation_bitfield be a byte array filled with zeros of length (len(committee) + 7) // 8.
   Let index_into_committee be the index into the validator's committee at which validator_index is
     located.
   Set aggregation_bitfield[index_into_committee // 8] |= 2 ** (index_into_committee % 8).
  */
  private BytesValue getParticipationBitfield(
      ValidatorIndex index, List<ValidatorIndex> committee) {
    int indexIntoCommittee = committee.indexOf(index);
    assert indexIntoCommittee >= 0;

    int aggregationBitfieldSize = (committee.size() + 7) / 8;
    MutableBytesValue aggregationBitfield =
        MutableBytesValue.wrap(new byte[aggregationBitfieldSize]);
    int indexIntoBitfield = indexIntoCommittee / 8;
    aggregationBitfield.set(indexIntoBitfield, (byte) ((1 << (indexIntoCommittee % 8)) & 0xFF));
    return aggregationBitfield;
  }
}
