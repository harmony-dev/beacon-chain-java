package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
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

  private SpecHelpers specHelpers;

  public RandaoVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    ValidatorRecord proposer =
        state
            .getValidatorRegistry()
            .get(safeInt(specHelpers.get_beacon_proposer_index(state, state.getSlot())));

    if (!specHelpers.bls_verify(
        proposer.getPubKey(),
        Hash32.wrap(Bytes32.leftPad(proposer.getProposerSlots().toBytesBigEndian())),
        block.getRandaoReveal(),
        specHelpers.get_domain(state.getForkData(), state.getSlot(), RANDAO))) {

      return VerificationResult.failedResult("Incorrect RANDAO reveal");
    }

    return VerificationResult.PASSED;
  }
}
