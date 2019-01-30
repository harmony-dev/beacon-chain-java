package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;

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

  public AttestationVerifier(ChainSpec chainSpec, SpecHelpers specHelpers) {
    this.chainSpec = chainSpec;
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(Attestation attestation, BeaconState state) {
    AttestationData data = attestation.getData();

    specHelpers.checkShardRange(data.getShard());

    if (data.getSlot().plus(chainSpec.getMinAttestationInclusionDelay()).compareTo(state.getSlot())
        > 0) {
      return failedResult(
          "MIN_ATTESTATION_INCLUSION_DELAY violated, inclusion slot starts from %s but got %s",
          data.getSlot().plus(chainSpec.getMinAttestationInclusionDelay()), state.getSlot());
    }

    if (data.getSlot().plus(chainSpec.getEpochLength()).compareTo(state.getSlot()) < 0) {
      return failedResult(
          "EPOCH_LENGTH boundary violated, boundary slot %d, attestation slot %s",
          Math.max(0, state.getSlot().getValue() - chainSpec.getEpochLength().getValue()),
          data.getSlot());
    }

    if (data.getSlot()
            .compareTo(state.getSlot().minus(state.getSlot().modulo(chainSpec.getEpochLength())))
        >= 0) {
      if (!data.getSlot().equals(state.getJustifiedSlot())) {
        return failedResult(
            "attestation_data.slot=%s must be equal to justified_slot=%s",
            data.getSlot(), state.getJustifiedSlot());
      }
    } else {
      if (!data.getSlot().equals(state.getPreviousJustifiedSlot())) {
        return failedResult(
            "attestation_data.slot=%s must be equal to previous_justified_slot=%s",
            data.getSlot(), state.getPreviousJustifiedSlot());
      }
    }

    Hash32 blockRootAtJustifiedSlot = specHelpers.get_block_root(state, data.getJustifiedSlot());
    if (!data.getJustifiedBlockRoot().equals(blockRootAtJustifiedSlot)) {
      return failedResult(
          "attestation_data.justified_block_root must be equal to block_root at state.justified_slot, "
              + "justified_block_root=%s, block_root=%s",
          data.getJustifiedBlockRoot(), blockRootAtJustifiedSlot);
    }

    Hash32 shardBlockRoot =
        state.getLatestCrosslinks().get(data.getShard()).getShardBlockRoot();
    if (!data.getJustifiedBlockRoot().equals(shardBlockRoot)
        && !data.getShardBlockRoot().equals(shardBlockRoot)) {
      return failedResult(
          "either attestation_data.justified_block_root or attestation_data.shard_block_root must be "
              + "equal to latest_crosslink.shard_block_root, justified_block_root=%s, "
              + "attestation_data.shard_block_root=%s, latest_crosslink.shard_block_root=%s",
          data.getJustifiedBlockRoot(), data.getShardBlockRoot(), shardBlockRoot);
    }

    List<ValidatorIndex> participants =
        specHelpers.get_attestation_participants(
            state, data, attestation.getParticipationBitfield());

    List<BLSPubkey> pubKeys = specHelpers.mapIndicesToPubKeys(state, participants);
    PublicKey groupPublicKey = specHelpers.bls_aggregate_pubkeys(pubKeys);
    if (!specHelpers.bls_verify(
        groupPublicKey,
        specHelpers.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
        attestation.getAggregateSignature(),
        specHelpers.get_domain(state.getForkData(), data.getSlot(), ATTESTATION))) {
      return failedResult("failed to verify aggregated signature");
    }

    if (!Hash32.ZERO.equals(data.getShardBlockRoot())) {
      return failedResult(
          "attestation_data.shard_block_root must be equal to zero hash, phase 0 check");
    }

    return PASSED;
  }
}
