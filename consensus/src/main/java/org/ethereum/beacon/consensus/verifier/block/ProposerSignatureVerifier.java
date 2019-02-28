package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.Proposal;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;

/**
 * Verifies proposer signature of the block.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#proposer-signature>Proposer
 *     signature</a> in the spec.
 */
public class ProposerSignatureVerifier implements BeaconBlockVerifier {

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  public ProposerSignatureVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {

    // Let proposal_root = hash_tree_root(
    //  Proposal(state.slot, BEACON_CHAIN_SHARD_NUMBER, block_without_signature_root)).
    Proposal proposal =
        new Proposal(
            state.getSlot(),
            chainSpec.getBeaconChainShardNumber(),
            specHelpers.signed_root(block, "signature"),
            block.getSignature());
    Hash32 proposalRoot = specHelpers.signed_root(proposal, "signature");

    // Verify that bls_verify(
    //  pubkey=state.validator_registry[get_beacon_proposer_index(state, state.slot)].pubkey,
    //  message=proposal_root,
    //  signature=block.signature,
    //  domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_PROPOSAL)).
    ValidatorIndex proposerIndex = specHelpers.get_beacon_proposer_index(state, state.getSlot());
    BLSPubkey publicKey = state.getValidatorRegistry().get(proposerIndex).getPubKey();
    Bytes8 domain =
        specHelpers.get_domain(state.getForkData(), specHelpers.get_current_epoch(state), PROPOSAL);

    if (specHelpers.bls_verify(publicKey, proposalRoot, proposal.getSignature(), domain)) {
      return VerificationResult.PASSED;
    } else {
      return VerificationResult.failedResult(
          "Proposer signature verification has failed for block %s", block);
    }
  }
}
