package org.ethereum.beacon.consensus.verifier;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.uint.UInt24;

/**
 * Verifies proposer signature of the block.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#proposer-signature>Proposer
 *     signature</a> in the spec.
 */
public class ProposerSignatureVerifier implements BeaconBlockVerifier {

  private final ChainSpec chainSpec;
  private final SpecHelpers specHelpers;

  public ProposerSignatureVerifier(ChainSpec chainSpec) {
    this.chainSpec = chainSpec;
    this.specHelpers = new SpecHelpers(chainSpec);
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    BeaconBlock blockWithoutSignature =
        BeaconBlock.Builder.fromBlock(block).withSignature(chainSpec.getEmptySignature()).build();

    ProposalSignedData proposal =
        new ProposalSignedData(
            block.getSlot(),
            chainSpec.getBeaconChainShardNumber(),
            specHelpers.hash_tree_root(blockWithoutSignature));

    Hash32 proposalRoot = specHelpers.hash_tree_root(proposal);
    UInt24 proposerIndex = specHelpers.get_beacon_proposer_index(state, block.getSlot());
    Bytes48 publicKey = state.getValidatorRegistryUnsafe().get(safeInt(proposerIndex)).getPubKey();
    Bytes8 domain = specHelpers.get_domain(state.getForkData(), block.getSlot(), PROPOSAL);

    if (specHelpers.bls_verify(publicKey, proposalRoot, block.getSignature(), domain)) {
      return VerificationResult.PASSED;
    } else {
      return VerificationResult.createdFailed(
          "Proposer signature verification has failed for block %s", block);
    }
  }
}
