package org.ethereum.beacon.validator.attester;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.ValidatorService;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An implementation of beacon chain attester.
 *
 * @see BeaconChainAttester
 * @see ValidatorService
 */
public class BeaconChainAttesterImpl implements BeaconChainAttester {

  /** The spec. */
  private SpecHelpers specHelpers;
  /** Chain parameters. */
  private ChainSpec chainSpec;

  public BeaconChainAttesterImpl(SpecHelpers specHelpers, ChainSpec chainSpec) {
    this.specHelpers = specHelpers;
    this.chainSpec = chainSpec;
  }

  @Override
  public Attestation attest(
      ValidatorIndex validatorIndex,
      ShardNumber shard,
      ObservableBeaconState observableState,
      MessageSigner<BLSSignature> signer) {
    BeaconState state = observableState.getLatestSlotState();

    SlotNumber slot = state.getSlot();
    Hash32 beaconBlockRoot = specHelpers.hash_tree_root(observableState.getHead());
    Hash32 epochBoundaryRoot = getEpochBoundaryRoot(state, observableState.getHead());
    Hash32 shardBlockRoot = Hash32.ZERO; // Note: This is a stub for phase 0.
    Hash32 latestCrosslinkRoot = getLatestCrosslinkRoot(state, shard);
    EpochNumber justifiedEpoch = state.getJustifiedEpoch();
    Hash32 justifiedBlockRoot =
        getJustifiedBlockRoot(state, specHelpers.get_epoch_start_slot(justifiedEpoch));
    AttestationData data =
        new AttestationData(
            slot,
            shard,
            beaconBlockRoot,
            epochBoundaryRoot,
            shardBlockRoot,
            latestCrosslinkRoot,
            justifiedEpoch,
            justifiedBlockRoot);

    List<ValidatorIndex> committee = getCommittee(state, shard);
    BytesValue participationBitfield = getParticipationBitfield(validatorIndex, committee);
    BytesValue custodyBitfield = getCustodyBitfield(validatorIndex, committee);
    BLSSignature aggregateSignature = getAggregateSignature(state, data, signer);

    return new Attestation(
        data, Bitfield.of(participationBitfield), Bitfield.of(custodyBitfield), aggregateSignature);
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
    if (shard.equals(chainSpec.getBeaconChainShardNumber())) {
      return specHelpers.get_crosslink_committees_at_slot(state, state.getSlot()).get(0).getCommittee();
    } else {
      return specHelpers
          .get_crosslink_committees_at_slot(state, state.getSlot()).stream()
          .filter(sc -> sc.getShard().equals(shard))
          .findFirst()
          .map(sc -> sc.getCommittee())
          .orElse(Collections.emptyList());
    }
  }

  /*
   Note: This can be looked up in the state using
     get_block_root(state, head.slot - head.slot % EPOCH_LENGTH).
  */
  @VisibleForTesting
  Hash32 getEpochBoundaryRoot(BeaconState state, BeaconBlock head) {
    SlotNumber ancestorMaxSlot = head.getSlot().decrement();
    SlotNumber epochBoundarySlot =
        ancestorMaxSlot.minus(ancestorMaxSlot.modulo(chainSpec.getEpochLength()));
    return specHelpers.get_block_root(state, epochBoundarySlot);
  }

  /*
   Set attestation_data.latest_crosslink_root = state.latest_crosslinks[shard].shard_block_root
     where state is the beacon state at head and shard is the validator's assigned shard.
  */
  @VisibleForTesting
  Hash32 getLatestCrosslinkRoot(BeaconState state, ShardNumber shard) {
    if (shard.equals(chainSpec.getBeaconChainShardNumber())) {
      return Hash32.ZERO;
    } else {
      return state.getLatestCrosslinks().get(shard).getShardBlockRoot();
    }
  }

  /*
   Set attestation_data.justified_block_root = hash_tree_root(justified_block)
     where justified_block is the block at state.justified_slot in the chain defined by head.

   Note: This can be looked up in the state using get_block_root(state, justified_slot).
  */
  @VisibleForTesting
  Hash32 getJustifiedBlockRoot(BeaconState state, SlotNumber slot) {
    return specHelpers.get_block_root(state, slot);
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
    Hash32 hash = specHelpers.hash_tree_root(attestationDataAndCustodyBit);
    Bytes8 domain = specHelpers.get_domain(state.getForkData(),
        specHelpers.get_current_epoch(state), ATTESTATION);
    return signer.sign(hash, domain);
  }
}
