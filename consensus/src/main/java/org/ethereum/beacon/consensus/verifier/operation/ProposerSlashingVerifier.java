package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_BLOCK;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.state.ValidatorRecord;

/**
 * Verifies {@link ProposerSlashing} beacon chain operation.
 *
 * @see ProposerSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#proposer-slashings-1">Proposer
 *     slashings</a> in the spec.
 */
public class ProposerSlashingVerifier implements OperationVerifier<ProposerSlashing> {

  private SpecHelpers spec;

  public ProposerSlashingVerifier(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(ProposerSlashing proposerSlashing, BeaconState state) {
    spec.checkIndexRange(state, proposerSlashing.getProposerIndex());

    if (!spec.slot_to_epoch(proposerSlashing
        .getHeader1()
        .getSlot())
        .equals(spec.slot_to_epoch(proposerSlashing.getHeader2().getSlot()))) {
      return failedResult("proposer_slashing.header_1.epoch != proposer_slashing.header_2.epoch");
    }

    if (proposerSlashing
        .getHeader1()
        .equals(proposerSlashing.getHeader2())) {
      return failedResult("proposer_slashing.header_1 == proposer_slashing.header_2");
    }

    ValidatorRecord proposer =
        state.getValidatorRegistry().get(proposerSlashing.getProposerIndex());
    if (proposer.getSlashed()) {
      return failedResult(
          "proposer was already slashed");
    }

    if (!spec.bls_verify(
        proposer.getPubKey(),
        spec.signed_root(proposerSlashing.getHeader1(), "signature"),
        proposerSlashing.getHeader1().getSignature(),
        spec.get_domain(
            state.getFork(),
            spec.slot_to_epoch(proposerSlashing.getHeader1().getSlot()),
            BEACON_BLOCK))) {
      return failedResult("header_1.signature is invalid");
    }

    if (!spec.bls_verify(
        proposer.getPubKey(),
        spec.signed_root(proposerSlashing.getHeader2(), "signature"),
        proposerSlashing.getHeader2().getSignature(),
        spec.get_domain(
            state.getFork(),
            spec.slot_to_epoch(proposerSlashing.getHeader2().getSlot()),
            BEACON_BLOCK))) {
      return failedResult("header_2.signature is invalid");
    }

    return PASSED;
  }
}
