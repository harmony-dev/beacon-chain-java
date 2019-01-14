package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.createdFailed;
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
  public VerificationResult verify(ProposerSlashing operation, BeaconState state) {
    if (safeInt(operation.getProposerIndex()) >= state.getValidatorRegistryUnsafe().size()) {
      return createdFailed(
          "proposer index is out of range: %d while registry size %d",
          operation.getProposerIndex(), state.getValidatorRegistryUnsafe().size());
    }

    if (operation.getProposalData1().getSlot().equals(operation.getProposalData2().getSlot())) {
      return createdFailed("proposal_data_1.slot != proposal_data_2.slot");
    }

    if (operation.getProposalData1().getShard().equals(operation.getProposalData2().getShard())) {
      return createdFailed("proposal_data_1.shard != proposal_data_2.shard");
    }

    if (operation
        .getProposalData1()
        .getBlockRoot()
        .equals(operation.getProposalData2().getBlockRoot())) {
      return createdFailed(
          "proposal_data_1.block_root == proposal_data_2.block_root, roots should not be equal");
    }

    ValidatorRecord proposer =
        state.getValidatorRegistryUnsafe().get(safeInt(operation.getProposerIndex()));
    if (proposer.getPenalizedSlot().compareTo(state.getSlot()) >= 0) {
      return createdFailed(
          "proposer penalized_slot should be less than state.slot, got penalized_slot=%d, state.slot=%d",
          proposer.getPenalizedSlot(), state.getSlot());
    }

    if (specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.hash_tree_root(operation.getProposalData1()),
        operation.getProposalSignature1(),
        specHelpers.get_domain(
            state.getForkData(), operation.getProposalData1().getSlot(), PROPOSAL))) {
      return createdFailed("proposal_signature_1 is invalid");
    }

    if (specHelpers.bls_verify(
        proposer.getPubKey(),
        specHelpers.hash_tree_root(operation.getProposalData2()),
        operation.getProposalSignature2(),
        specHelpers.get_domain(
            state.getForkData(), operation.getProposalData1().getSlot(), PROPOSAL))) {
      return createdFailed("proposal_signature_1 is invalid");
    }

    return PASSED;
  }
}
