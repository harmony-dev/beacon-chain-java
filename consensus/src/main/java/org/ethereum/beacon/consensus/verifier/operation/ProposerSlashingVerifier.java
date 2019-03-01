package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;

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

  private SpecHelpers specHelpers;

  public ProposerSlashingVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(ProposerSlashing proposerSlashing, BeaconState state) {
    specHelpers.checkIndexRange(state, proposerSlashing.getProposerIndex());
    specHelpers.checkShardRange(proposerSlashing.getProposal1().getShard());
    specHelpers.checkShardRange(proposerSlashing.getProposal2().getShard());

    if (!proposerSlashing
        .getProposal1()
        .getSlot()
        .equals(proposerSlashing.getProposal2().getSlot())) {
      return failedResult("proposal_data_1.slot != proposal_data_2.slot");
    }

    if (!proposerSlashing
        .getProposal1()
        .getShard()
        .equals(proposerSlashing.getProposal2().getShard())) {
      return failedResult("proposal_data_1.shard != proposal_data_2.shard");
    }

    if (proposerSlashing
        .getProposal1()
        .getBlockRoot()
        .equals(proposerSlashing.getProposal2().getBlockRoot())) {
      return failedResult(
          "proposal_data_1.block_root == proposal_data_2.block_root, roots should not be equal");
    }

    ValidatorRecord proposer =
        state.getValidatorRegistry().get(proposerSlashing.getProposerIndex());
    if (proposer.getSlashed()) {
      return failedResult(
          "proposer was already slashed");
    }

    if (!specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.signed_root(proposerSlashing.getProposal1(), "signature"),
        proposerSlashing.getProposal1().getSignature(),
        specHelpers.get_domain(
            state.getForkData(),
            specHelpers.slot_to_epoch(proposerSlashing.getProposal1().getSlot()),
            PROPOSAL))) {
      return failedResult("proposal_1.signature is invalid");
    }

    if (!specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.signed_root(proposerSlashing.getProposal2(), "signature"),
        proposerSlashing.getProposal2().getSignature(),
        specHelpers.get_domain(
            state.getForkData(),
            specHelpers.slot_to_epoch(proposerSlashing.getProposal2().getSlot()),
            PROPOSAL))) {
      return failedResult("proposal_2.signature is invalid");
    }

    return PASSED;
  }
}
