package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Verifies {@link Attestation} beacon chain operation.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestations-1">Attesations</a>
 *     in the spec.
 */
public class AttestationVerifier implements OperationVerifier<Attestation> {

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  public AttestationVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
  }

  @Override
  public VerificationResult verify(Attestation attestation, BeaconState state) {
    AttestationData data = attestation.getData();

    specHelpers.checkShardRange(data.getShard());

    // Verify that attestation.data.slot <= state.slot - MIN_ATTESTATION_INCLUSION_DELAY
    //    < attestation.data.slot + SLOTS_PER_EPOCH
    if (!(data.getSlot()
            .lessEqual(state.getSlot().minus(chainSpec.getMinAttestationInclusionDelay()))
        && state
            .getSlot()
            .minus(chainSpec.getMinAttestationInclusionDelay())
            .less(data.getSlot().plus(chainSpec.getSlotsPerEpoch())))) {

      return failedResult(
          "MIN_ATTESTATION_INCLUSION_DELAY violated, inclusion slot starts from %s but got %s",
          data.getSlot().plus(chainSpec.getMinAttestationInclusionDelay()), state.getSlot());
    }

    // Verify that attestation.data.justified_epoch is equal to
    // state.justified_epoch
    // if slot_to_epoch(attestation.data.slot + 1) >= get_current_epoch(state)
    // else state.previous_justified_epoch.
    if (!data.getJustifiedEpoch().equals(
        specHelpers.slot_to_epoch(data.getSlot().increment()).greaterEqual(specHelpers.get_current_epoch(state)) ?
        state.getJustifiedEpoch() : state.getPreviousJustifiedEpoch())) {
      return failedResult(
          "Attestation.data.justified_epoch is invalid");
    }

    // Verify that attestation.data.justified_block_root is equal to
    // get_block_root(state, get_epoch_start_slot(attestation.data.justified_epoch))
    Hash32 blockRootAtJustifiedSlot = specHelpers.get_block_root(state,
        specHelpers.get_epoch_start_slot(data.getJustifiedEpoch()));
    if (!data.getJustifiedBlockRoot().equals(blockRootAtJustifiedSlot)) {
      return failedResult(
          "attestation_data.justified_block_root must be equal to block_root at state.justified_slot, "
              + "justified_block_root=%s, block_root=%s",
          data.getJustifiedBlockRoot(), blockRootAtJustifiedSlot);
    }

    // Verify that either attestation.data.latest_crosslink_root or
    //  attestation.data.crosslink_data_root equals state.latest_crosslinks[shard].crosslink_data_root
    Hash32 crosslinkDataRoot =
        state.getLatestCrosslinks().get(data.getShard()).getCrosslinkDataRoot();
    if (!data.getLatestCrosslink().getCrosslinkDataRoot().equals(crosslinkDataRoot)
        && !data.getCrosslinkDataRoot().equals(crosslinkDataRoot)) {
      return failedResult(
          "either attestation_data.justified_block_root or attestation_data.crosslink_data_root must be "
              + "equal to latest_crosslink.crosslink_data_root, justified_block_root=%s, "
              + "attestation_data.crosslink_data_root=%s, latest_crosslink.crosslink_data_root=%s",
          data.getJustifiedBlockRoot(), data.getCrosslinkDataRoot(), crosslinkDataRoot);
    }

    // Verify bitfields and aggregate signature:

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
    Optional<ShardCommittee> crosslink_committee_opt = specHelpers
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
        specHelpers.get_attestation_participants(state, data, attestation.getAggregationBitfield());

    //  custody_bit_1_participants = get_attestation_participants(state, attestation.data, attestation.custody_bitfield)
    List<ValidatorIndex> custody_bit_1_participants =
        specHelpers.get_attestation_participants(state, data, attestation.getCustodyBitfield());
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
    List<BLSPubkey> pubKeys1 = specHelpers.mapIndicesToPubKeys(state, custody_bit_0_participants);
    PublicKey groupPublicKey1 = specHelpers.bls_aggregate_pubkeys(pubKeys1);
    List<BLSPubkey> pubKeys2 = specHelpers.mapIndicesToPubKeys(state, custody_bit_1_participants);
    PublicKey groupPublicKey2 = specHelpers.bls_aggregate_pubkeys(pubKeys2);
    if (!specHelpers.bls_verify_multiple(
        Arrays.asList(groupPublicKey1, groupPublicKey2),
        Arrays.asList(
          specHelpers.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
          specHelpers.hash_tree_root(new AttestationDataAndCustodyBit(data, true))),
        attestation.getAggregateSignature(),
        specHelpers.get_domain(state.getForkData(), specHelpers.slot_to_epoch(data.getSlot()), ATTESTATION))) {
      return failedResult("failed to verify aggregated signature");
    }

    if (!Hash32.ZERO.equals(data.getCrosslinkDataRoot())) {
      return failedResult(
          "attestation_data.crosslink_data_root must be equal to zero hash, phase 0 check");
    }

    return PASSED;
  }
}
