package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.collections.ReadList;

/**
 * Verifies {@link AttesterSlashing} beacon chain operation.
 *
 * @see AttesterSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attester-slashings-1">
 *     Attester slashings</a> in the spec.
 */
public class AttesterSlashingVerifier implements OperationVerifier<AttesterSlashing> {

  private SpecHelpers specHelpers;

  public AttesterSlashingVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(AttesterSlashing attesterSlashing, BeaconState state) {
    SlashableAttestation slashableAttestation1 = attesterSlashing.getSlashableAttestation1();
    SlashableAttestation slashableAttestation2 = attesterSlashing.getSlashableAttestation2();

    specHelpers.checkIndexRange(state, slashableAttestation1.getValidatorIndices());
    specHelpers.checkIndexRange(state, slashableAttestation2.getValidatorIndices());
    specHelpers.checkShardRange(slashableAttestation1.getData().getShard());
    specHelpers.checkShardRange(slashableAttestation2.getData().getShard());

    if (slashableAttestation1.getData().equals(slashableAttestation2.getData())) {
      return failedResult("slashable_vote_data_1 != slashable_vote_data_2");
    }

    if (!(specHelpers.is_double_vote(slashableAttestation1.getData(), slashableAttestation2.getData())
        || specHelpers.is_surround_vote(
          slashableAttestation1.getData(), slashableAttestation2.getData()))) {
      return failedResult("no slashing conditions found");
    }

    if (!specHelpers.verify_slashable_attestation(state, slashableAttestation1)) {
      return failedResult("slashableAttestation1 is incorrect");
    }

    if (!specHelpers.verify_slashable_attestation(state, slashableAttestation2)) {
      return failedResult("slashableAttestation2 is incorrect");
    }

    ReadList<Integer, ValidatorIndex> intersection =
        slashableAttestation1.getValidatorIndices().intersection(
            slashableAttestation1.getValidatorIndices());
    if (intersection.stream()
        .noneMatch(i -> state.getValidatorRegistry().get(i).getSlashed())) {
      return failedResult("spec assertion failed");
    }

    return PASSED;
  }
}
