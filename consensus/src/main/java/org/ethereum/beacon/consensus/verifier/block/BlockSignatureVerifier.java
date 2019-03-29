package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_BLOCK;

/**
 * Verifies proposer signature of the block.
 *
 * @see <a
 *     href=https://github.com/ethereum/eth2.0-specs/blob/0.4.0/specs/core/0_beacon-chain.md#block-signature"">Block
 *     signature</a> in the spec.
 */
public class BlockSignatureVerifier implements BeaconBlockVerifier {

  private BeaconChainSpec spec;

  public BlockSignatureVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    Hash32 headerRoot = spec.signed_root(block);

    // Verify that bls_verify(
    //  pubkey=state.validator_registry[get_beacon_proposer_index(state, state.slot)].pubkey,
    //  message=proposal_root,
    //  signature=block.signature,
    //  domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_PROPOSAL)).
    ValidatorIndex proposerIndex = spec.get_beacon_proposer_index(state, state.getSlot());
    BLSPubkey publicKey = state.getValidatorRegistry().get(proposerIndex).getPubKey();
    UInt64 domain =
        spec.get_domain(state.getFork(), spec.get_current_epoch(state), BEACON_BLOCK);

    if (spec.bls_verify(publicKey, headerRoot, block.getSignature(), domain)) {
      return VerificationResult.PASSED;
    } else {
      return VerificationResult.failedResult(
          "Proposer signature verification has failed for block %s", block);
    }
  }
}
