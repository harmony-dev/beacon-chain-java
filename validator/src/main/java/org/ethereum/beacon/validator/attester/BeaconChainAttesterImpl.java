package org.ethereum.beacon.validator.attester;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

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
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainAttesterImpl implements BeaconChainAttester {

  private SpecHelpers specHelpers;
  private ChainSpec chainSpec;

  public BeaconChainAttesterImpl(SpecHelpers specHelpers, ChainSpec chainSpec) {
    this.specHelpers = specHelpers;
    this.chainSpec = chainSpec;
  }

  @Override
  public Attestation attest(
      UInt24 index,
      List<UInt24> committee,
      UInt64 shard,
      ObservableBeaconState observableState,
      MessageSigner<Bytes96> signer) {
    BeaconState state = observableState.getLatestSlotState();

    UInt64 slot = state.getSlot();
    Hash32 beaconBlockRoot = specHelpers.hash_tree_root(observableState.getHead());
    Hash32 epochBoundaryRoot = getEpochBoundaryRoot(state, observableState.getHead());
    Hash32 shardBlockRoot = Hash32.ZERO; // Note: This is a stub for phase 0.
    Hash32 latestCrosslinkRoot = getLatestCrosslinkRoot(state, shard);
    UInt64 justifiedSlot = state.getJustifiedSlot();
    Hash32 justifiedBlockRoot = getJustifiedBlockRoot(state, justifiedSlot);
    AttestationData data =
        new AttestationData(
            slot,
            shard,
            beaconBlockRoot,
            epochBoundaryRoot,
            shardBlockRoot,
            latestCrosslinkRoot,
            justifiedSlot,
            justifiedBlockRoot);

    BytesValue participationBitfield = getParticipationBitfield(index, committee);
    BytesValue custodyBitfield = getCustodyBitfield(index, committee);
    Bytes96 aggregateSignature = getAggregateSignature(state, data, signer);

    return new Attestation(data, participationBitfield, custodyBitfield, aggregateSignature);
  }

  /*
   Note: This can be looked up in the state using
     get_block_root(state, head.slot - head.slot % EPOCH_LENGTH).
  */
  private Hash32 getEpochBoundaryRoot(BeaconState state, BeaconBlock head) {
    UInt64 epochBoundarySlot =
        head.getSlot().minus(head.getSlot().modulo(chainSpec.getEpochLength()));
    return specHelpers.get_block_root(state, epochBoundarySlot);
  }

  /*
   Set attestation_data.latest_crosslink_root = state.latest_crosslinks[shard].shard_block_root
     where state is the beacon state at head and shard is the validator's assigned shard.
  */
  private Hash32 getLatestCrosslinkRoot(BeaconState state, UInt64 shard) {
    return state.getLatestCrosslinks().get(safeInt(shard)).getShardBlockRoot();
  }

  /*
   Set attestation_data.justified_block_root = hash_tree_root(justified_block)
     where justified_block is the block at state.justified_slot in the chain defined by head.

   Note: This can be looked up in the state using get_block_root(state, justified_slot).
  */
  private Hash32 getJustifiedBlockRoot(BeaconState state, UInt64 slot) {
    return specHelpers.get_block_root(state, slot);
  }

  /*
   Let aggregation_bitfield be a byte array filled with zeros of length (len(committee) + 7) // 8.
   Let index_into_committee be the index into the validator's committee at which validator_index is
     located.
   Set aggregation_bitfield[index_into_committee // 8] |= 2 ** (index_into_committee % 8).
  */
  private BytesValue getParticipationBitfield(UInt24 index, List<UInt24> committee) {
    int indexIntoCommittee = Collections.binarySearch(committee, index);
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
  private BytesValue getCustodyBitfield(UInt24 index, List<UInt24> committee) {
    int custodyBitfieldSize = (committee.size() + 7) / 8;
    return BytesValue.wrap(new byte[custodyBitfieldSize]);
  }

  private Bytes96 getAggregateSignature(
      BeaconState state, AttestationData data, MessageSigner<Bytes96> signer) {
    AttestationDataAndCustodyBit attestationDataAndCustodyBit =
        new AttestationDataAndCustodyBit(data, false);
    Hash32 hash = specHelpers.hash_tree_root(attestationDataAndCustodyBit);
    Bytes8 domain = specHelpers.get_domain(state.getForkData(), state.getSlot(), ATTESTATION);
    return signer.sign(hash, domain);
  }
}
