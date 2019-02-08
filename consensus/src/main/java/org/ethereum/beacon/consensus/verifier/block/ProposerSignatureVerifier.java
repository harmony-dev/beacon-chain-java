package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

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

  public ProposerSignatureVerifier(ChainSpec chainSpec, SpecHelpers specHelpers) {
    this.chainSpec = chainSpec;
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {

    // Let block_without_signature_root be the hash_tree_root of block where block.signature is set to EMPTY_SIGNATURE.
    BeaconBlock blockWithoutSignature = block.withoutSignature();

    // Let proposal_root = hash_tree_root(
    //  ProposalSignedData(state.slot, BEACON_CHAIN_SHARD_NUMBER, block_without_signature_root)).
    ProposalSignedData proposal =
        new ProposalSignedData(
            state.getSlot(),
            chainSpec.getBeaconChainShardNumber(),
            specHelpers.hash_tree_root(blockWithoutSignature));
    Hash32 proposalRoot = specHelpers.hash_tree_root(proposal);

    // Verify that bls_verify(
    //  pubkey=state.validator_registry[get_beacon_proposer_index(state, state.slot)].pubkey,
    //  message=proposal_root,
    //  signature=block.signature,
    //  domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_PROPOSAL)).
    ValidatorIndex proposerIndex = specHelpers.get_beacon_proposer_index(state, state.getSlot());
    BLSPubkey publicKey = state.getValidatorRegistry().get(proposerIndex).getPubKey();
    Bytes8 domain = specHelpers.get_domain(state.getForkData(),
        specHelpers.get_current_epoch(state), PROPOSAL);

    if (specHelpers.bls_verify(publicKey, proposalRoot, block.getSignature(), domain)) {
      return VerificationResult.PASSED;
    } else {
      return VerificationResult.failedResult(
          "Proposer signature verification has failed for block %s", block);
    }
  }
}
