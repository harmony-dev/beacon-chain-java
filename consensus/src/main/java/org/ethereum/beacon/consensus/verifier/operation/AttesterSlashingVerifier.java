package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
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

  private BeaconChainSpec spec;

  public AttesterSlashingVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(AttesterSlashing attesterSlashing, BeaconState state) {
    IndexedAttestation indexedAttestation1 = attesterSlashing.getAttestation1();
    IndexedAttestation indexedAttestation2 = attesterSlashing.getAttestation2();

    spec.checkIndexRange(state, indexedAttestation1.getCustodyBit0Indices());
    spec.checkIndexRange(state, indexedAttestation1.getCustodyBit1Indices());
    spec.checkIndexRange(state, indexedAttestation2.getCustodyBit0Indices());
    spec.checkIndexRange(state, indexedAttestation2.getCustodyBit1Indices());
    spec.checkShardRange(indexedAttestation1.getData().getShard());
    spec.checkShardRange(indexedAttestation2.getData().getShard());

    if (indexedAttestation1.getData().equals(indexedAttestation2.getData())) {
      return failedResult("slashable_vote_data_1 != slashable_vote_data_2");
    }

    if (!(spec.is_double_vote(indexedAttestation1.getData(), indexedAttestation2.getData())
        || spec.is_surround_vote(
          indexedAttestation1.getData(), indexedAttestation2.getData()))) {
      return failedResult("no slashing conditions found");
    }

    if (!spec.verify_slashable_attestation(state, indexedAttestation1)) {
      return failedResult("indexedAttestation1 is incorrect");
    }

    if (!spec.verify_slashable_attestation(state, indexedAttestation2)) {
      return failedResult("indexedAttestation2 is incorrect");
    }

    ReadList<Integer, ValidatorIndex> intersection =
        indexedAttestation1.getCustodyBit0Indices().intersection(
            indexedAttestation2.getCustodyBit0Indices());
    if (intersection.stream()
        .noneMatch(i -> state.getValidatorRegistry().get(i).getSlashed())) {
      return failedResult("spec assertion failed");
    }

    return PASSED;
  }
}
