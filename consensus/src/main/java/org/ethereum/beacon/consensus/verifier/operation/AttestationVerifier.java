package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;

/**
 * Verifies {@link Attestation} beacon chain operation.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestations-1">Attesations</a>
 *     in the spec.
 */
public class AttestationVerifier implements OperationVerifier<Attestation> {

  private BeaconChainSpec spec;

  public AttestationVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Attestation attestation, BeaconState state) {
    AttestationData data = attestation.getData();

    if (attestation.getData().getSlot().less(spec.getConstants().getGenesisSlot())) {
      return failedResult("Attestation slot %s is less than GENESIS_SLOT %s",
          attestation.getData().getSlot(), spec.getConstants().getGenesisSlot());
    }

    spec.checkShardRange(data.getShard());

    // Verify that attestation.data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot
    //    < attestation.data.slot + SLOTS_PER_EPOCH
    if (state.getSlot()
        .less(data.getSlot().plus(spec.getConstants().getMinAttestationInclusionDelay()))) {
      return failedResult(
          "MIN_ATTESTATION_INCLUSION_DELAY violated, inclusion slot starts from %s but got %s",
          data.getSlot().plus(spec.getConstants().getMinAttestationInclusionDelay()), state.getSlot());
    }
    if (state.getSlot().greaterEqual(data.getSlot().plus(spec.getConstants().getSlotsPerEpoch()))) {
      return failedResult(
          "MIN_ATTESTATION_INCLUSION_DELAY violated, inclusion slot limit is %s but got %s",
          data.getSlot().plus(spec.getConstants().getSlotsPerEpoch()).decrement(),
          state.getSlot());
    }

    // # Can't submit attestations too quickly
    // assert attestation.data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot
    if (!data.getSlot().plus(spec.getConstants().getMinAttestationInclusionDelay())
        .lessEqual(state.getSlot())) {
      return failedResult(
          "attestation inclusion upper limit violated, inclusion slot ends with %s but got %s",
          state.getSlot(),
          data.getSlot().plus(spec.getConstants().getMinAttestationInclusionDelay()));
    }

    /* # Verify that the justified epoch and root is correct
    if slot_to_epoch(attestation.data.slot) >= get_current_epoch(state):
        # Case 1: current epoch attestations
        assert attestation.data.source_epoch == state.current_justified_epoch
        assert attestation.data.source_root == state.current_justified_root
    else:
        # Case 2: previous epoch attestations
        assert attestation.data.source_epoch == state.previous_justified_epoch
        assert attestation.data.source_root == state.previous_justified_root */

    if (spec.slot_to_epoch(data.getSlot()).greaterEqual(spec.get_current_epoch(state))) {
      if (!(data.getSourceEpoch().equals(state.getCurrentJustifiedEpoch()))) {
        return failedResult("case 1: source_epoch doesn't match, expected %s but got %s",
            state.getCurrentJustifiedEpoch(), data.getSourceEpoch());
      }
      if (!(data.getSourceRoot().equals(state.getCurrentJustifiedRoot()))) {
        return failedResult("case 1: source_root doesn't match, expected %s but got %s",
            state.getCurrentJustifiedRoot(), data.getSourceRoot());
      }
    } else {
      if (!(data.getSourceEpoch().equals(state.getPreviousJustifiedEpoch()))) {
        return failedResult("case 2: source_epoch doesn't match, expected %s but got %s",
            state.getPreviousJustifiedEpoch(), data.getSourceEpoch());
      }
      if (!(data.getSourceRoot().equals(state.getPreviousJustifiedRoot()))) {
        return failedResult("case 2: source_root doesn't match, expected %s but got %s",
            state.getPreviousJustifiedRoot(), data.getSourceRoot());
      }
    }

    // Check crosslink data
    /*  assert attestation.data.crosslink_data_root == ZERO_HASH  # [to be removed in phase 1]
        crosslinks = state.current_crosslinks if slot_to_epoch(attestation.data.slot) == get_current_epoch(state) else state.previous_crosslinks
        assert crosslinks[attestation.data.shard] == attestation.data.previous_crosslink */
    ReadList<ShardNumber, Crosslink> crosslinks =
        spec.slot_to_epoch(data.getSlot()).equals(spec.get_current_epoch(state)) ?
            state.getCurrentCrosslinks() : state.getPreviousCrosslinks();
    if (!spec.hash_tree_root(crosslinks.get(data.getShard())).equals(data.getPreviousCrosslinkRoot())) {
      return failedResult("attestation.data.latest_crosslink is incorrect");
    }

    if (!Hash32.ZERO.equals(data.getCrosslinkDataRoot())) {
      return failedResult(
          "attestation_data.crosslink_data_root must be equal to zero hash, phase 0 check");
    }

    //  assert attestation.custody_bitfield == b'\x00' * len(attestation.custody_bitfield)  # [TO BE REMOVED IN PHASE 1]
    if (!attestation.getCustodyBitfield().isZero()) {
      return failedResult("attestation.custody_bitfield != ZERO");
    }
    //  assert attestation.aggregation_bitfield != b'\x00' * len(attestation.aggregation_bitfield)
    if (attestation.getAggregationBitfield().isZero()) {
      return failedResult("attestation.aggregation_bitfield == ZERO");
    }

    //  crosslink_committee = [
    //      committee for committee, shard in get_crosslink_committees_at_slot(state, attestation.data.slot)
    //      if shard == attestation.data.shard
    //  ][0]
    Optional<ShardCommittee> crosslink_committee_opt = spec
        .get_crosslink_committees_at_slot(state, data.getSlot()).stream()
        .filter(c -> c.getShard().equals(data.getShard()))
        .findFirst();
    if (!crosslink_committee_opt.isPresent()) {
      return failedResult("crosslink_committee not found");
    }
    List<ValidatorIndex> crosslink_committee = crosslink_committee_opt.get().getCommittee();

    //  for i in range(len(crosslink_committee)):
    //      if get_bitfield_bit(attestation.aggregation_bitfield, i) == 0b0:
    //          assert get_bitfield_bit(attestation.custody_bitfield, i) == 0b0
    for (int i = 0; i < crosslink_committee.size(); i++) {
      if (attestation.getAggregationBitfield().getBit(i) == false) {
        if (attestation.getCustodyBitfield().getBit(i) != false) {
          return failedResult("aggregation_bitfield and custody_bitfield doesn't match");
        }
      }
    }

    //  participants = get_attestation_participants(state, attestation.data, attestation.aggregation_bitfield)
    List<ValidatorIndex> participants =
        spec.get_attestation_participants(state, data, attestation.getAggregationBitfield());

    //  custody_bit_1_participants = get_attestation_participants(state, attestation.data, attestation.custody_bitfield)
    List<ValidatorIndex> custody_bit_1_participants =
        spec.get_attestation_participants(state, data, attestation.getCustodyBitfield());
    //  custody_bit_0_participants = [i in participants for i not in custody_bit_1_participants]
    List<ValidatorIndex> custody_bit_0_participants = participants.stream()
        .filter(i -> !custody_bit_1_participants.contains(i)).collect(Collectors.toList());

    //  assert bls_verify_multiple(
    //      pubkeys=[
    //          bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_0_participants]),
    //          bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_1_participants]),
    //      ],
    //      messages=[
    //          hash_tree_root(AttestationDataAndCustodyBit(data=attestation.data, custody_bit=0b0)),
    //          hash_tree_root(AttestationDataAndCustodyBit(data=attestation.data, custody_bit=0b1)),
    //      ],
    //      signature=attestation.aggregate_signature,
    //      domain=get_domain(state.fork, slot_to_epoch(attestation.data.slot), DOMAIN_ATTESTATION),
    //  )
    List<BLSPubkey> pubKeys1 = spec.mapIndicesToPubKeys(state, custody_bit_0_participants);
    PublicKey groupPublicKey1 = spec.bls_aggregate_pubkeys(pubKeys1);
    List<BLSPubkey> pubKeys2 = spec.mapIndicesToPubKeys(state, custody_bit_1_participants);
    PublicKey groupPublicKey2 = spec.bls_aggregate_pubkeys(pubKeys2);
    if (!spec.bls_verify_multiple(
        Arrays.asList(groupPublicKey1, groupPublicKey2),
        Arrays.asList(
          spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
          spec.hash_tree_root(new AttestationDataAndCustodyBit(data, true))),
        attestation.getSignature(),
        spec.get_domain(state.getFork(), spec.slot_to_epoch(data.getSlot()), ATTESTATION))) {
      return failedResult("failed to verify aggregated signature");
    }

    return PASSED;
  }
}
