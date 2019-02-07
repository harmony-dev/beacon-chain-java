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
    specHelpers.checkShardRange(proposerSlashing.getProposalData1().getShard());
    specHelpers.checkShardRange(proposerSlashing.getProposalData2().getShard());

    if (!proposerSlashing
        .getProposalData1()
        .getSlot()
        .equals(proposerSlashing.getProposalData2().getSlot())) {
      return failedResult("proposal_data_1.slot != proposal_data_2.slot");
    }

    if (!proposerSlashing
        .getProposalData1()
        .getShard()
        .equals(proposerSlashing.getProposalData2().getShard())) {
      return failedResult("proposal_data_1.shard != proposal_data_2.shard");
    }

    if (proposerSlashing
        .getProposalData1()
        .getBlockRoot()
        .equals(proposerSlashing.getProposalData2().getBlockRoot())) {
      return failedResult(
          "proposal_data_1.block_root == proposal_data_2.block_root, roots should not be equal");
    }

    ValidatorRecord proposer =
        state.getValidatorRegistry().get(proposerSlashing.getProposerIndex());
    if (!proposer.getPenalizedEpoch().greater(specHelpers.get_current_epoch(state))) {
      return failedResult(
          "proposer penalized_epoch should be less than get_current_epoch(state)");
    }

    if (!specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.hash_tree_root(proposerSlashing.getProposalData1()),
        proposerSlashing.getProposalSignature1(),
        specHelpers.get_domain(
            state.getForkData(), proposerSlashing.getProposalData1().getSlot(), PROPOSAL))) {
      return failedResult("proposal_signature_1 is invalid");
    }

    if (!specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.hash_tree_root(proposerSlashing.getProposalData2()),
        proposerSlashing.getProposalSignature2(),
        specHelpers.get_domain(
            state.getForkData(), proposerSlashing.getProposalData2().getSlot(), PROPOSAL))) {
      return failedResult("proposal_signature_2 is invalid");
    }

    return PASSED;
  }
}
