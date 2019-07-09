package org.ethereum.beacon.validator.attester;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitlist;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.ValidatorService;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

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
      ValidatorIndex validatorIndex,
      ShardNumber shard,
      ObservableBeaconState observableState,
      MessageSigner<BLSSignature> signer) {
    BeaconState state = observableState.getLatestSlotState();

    Hash32 beaconBlockRoot = spec.signing_root(observableState.getHead());
    EpochNumber targetEpoch = spec.get_current_epoch(state);
    Checkpoint target = getTarget(state, observableState.getHead(), targetEpoch);
    Checkpoint source = getSource(state);
    Crosslink crosslink = getCrosslink(state, shard, targetEpoch);
    AttestationData data =
        new AttestationData(
            beaconBlockRoot, source, target, crosslink);

    List<ValidatorIndex> committee = getCommittee(state, shard);
    BytesValue participationBitfield = getParticipationBitfield(validatorIndex, committee);
    Bitlist participation = Bitlist.of(committee.size(), participationBitfield, spec.getConstants().getMaxValidatorsPerCommittee().intValue());
    BytesValue custodyBitfield = getCustodyBitfield(validatorIndex, committee);
    Bitlist custody = Bitlist.of(committee.size(), custodyBitfield, spec.getConstants().getMaxValidatorsPerCommittee().intValue());
    BLSSignature aggregateSignature = getAggregateSignature(state, data, signer);

    return new Attestation(participation, data, custody, aggregateSignature);
  }

  /**
   * Returns a committee at a state slot for a given shard.
   *
   * @param state a state.
   * @param shard a shard.
   * @return a committee.
   */
  @VisibleForTesting
  List<ValidatorIndex> getCommittee(BeaconState state, ShardNumber shard) {
    EpochNumber epoch = spec.get_current_epoch(state);
    return spec.get_crosslink_committee(state, epoch, shard);
  }

  @VisibleForTesting
  Checkpoint getTarget(BeaconState state, BeaconBlock head, EpochNumber targetEpoch) {
    SlotNumber epochBoundarySlot = spec.compute_start_slot_of_epoch(spec.compute_epoch_of_slot(head.getSlot()));
    if (epochBoundarySlot.equals(head.getSlot())) {
      return new Checkpoint(targetEpoch, spec.signing_root(head));
    } else {
      return new Checkpoint(targetEpoch, spec.get_block_root_at_slot(state, epochBoundarySlot));
    }
  }

  @VisibleForTesting
  Checkpoint getSource(BeaconState state) {
    return state.getCurrentJustifiedCheckpoint();
  }

  private Crosslink getCrosslink(BeaconState state, ShardNumber shard, EpochNumber targetEpoch) {
    Hash32 dataRoot = Hash32.ZERO; // Note: This is a stub for phase 0.
    Crosslink parentCrosslink = state.getCurrentCrosslinks().get(shard);
    Hash32 parentRoot = spec.hash_tree_root(parentCrosslink);
    EpochNumber startEpoch = parentCrosslink.getEndEpoch();
    EpochNumber endEpoch =
        UInt64s.min(
            targetEpoch,
            parentCrosslink.getEndEpoch().plus(spec.getConstants().getMaxEpochsPerCrosslink()));

    return new Crosslink(shard, parentRoot, startEpoch, endEpoch, dataRoot);
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

  /*
   Let custody_bitfield be a byte array filled with zeros of length (len(committee) + 7) // 8.
  */
  private BytesValue getCustodyBitfield(ValidatorIndex index, List<ValidatorIndex> committee) {
    int custodyBitfieldSize = (committee.size() + 7) / 8;
    return BytesValue.wrap(new byte[custodyBitfieldSize]);
  }

  /**
   * Creates {@link AttestationDataAndCustodyBit} instance and signs off on it.
   *
   * @param state a state at a slot that validator is attesting in.
   * @param data an attestation data instance.
   * @param signer message signer.
   * @return signature of attestation data and custody bit.
   */
  private BLSSignature getAggregateSignature(
      BeaconState state, AttestationData data, MessageSigner<BLSSignature> signer) {
    AttestationDataAndCustodyBit attestationDataAndCustodyBit =
        new AttestationDataAndCustodyBit(data, false);
    Hash32 hash = spec.hash_tree_root(attestationDataAndCustodyBit);
    UInt64 domain = spec.get_domain(state, ATTESTATION, data.getTarget().getEpoch());
    return signer.sign(hash, domain);
  }
}
