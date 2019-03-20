package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * Verifies RANDAO reveal.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#randao">RANDAO</a>
 *     in the spec.
 */
public class RandaoVerifier implements BeaconBlockVerifier {

  private SpecHelpers spec;

  public RandaoVerifier(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    // Let proposer = state.validator_registry[get_beacon_proposer_index(state, state.slot)].
    ValidatorRecord proposer =
        state
            .getValidatorRegistry()
            .get(spec.get_beacon_proposer_index(state, state.getSlot()));

    /*
     Verify that bls_verify(pubkey=proposer.pubkey,
       message=int_to_bytes32(get_current_epoch(state)), signature=block.randao_reveal,
       domain=get_domain(state.fork, get_current_epoch(state), DOMAIN_RANDAO))
    */
    if (!spec.bls_verify(
        proposer.getPubKey(),
        Hash32.wrap(Bytes32.leftPad(spec.get_current_epoch(state).toBytesBigEndian())),
        block.getBody().getRandaoReveal(),
        spec.get_domain(state.getFork(), spec.get_current_epoch(state), RANDAO))) {

      return VerificationResult.failedResult("RANDAO reveal verification failed");
    }

    return VerificationResult.PASSED;
  }
}
