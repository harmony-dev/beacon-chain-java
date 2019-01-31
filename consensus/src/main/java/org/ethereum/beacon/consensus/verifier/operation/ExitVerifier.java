package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.EXIT;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Verifies {@link Exit} beacon chain operation.
 *
 * @see Exit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#exits-1">Exits</a>
 *     in the spec.
 */
public class ExitVerifier implements OperationVerifier<Exit> {

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  public ExitVerifier(ChainSpec chainSpec, SpecHelpers specHelpers) {
    this.chainSpec = chainSpec;
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(Exit exit, BeaconState state) {
    specHelpers.checkIndexRange(state, exit.getValidatorIndex());

    ValidatorRecord validator = state.getValidatorRegistry().get(exit.getValidatorIndex());

    if (state.getSlot().plus(chainSpec.getEntryExitDelay()).lessEqual(validator.getExitSlot())) {
      return failedResult(
          "ENTRY_EXIT_DELAY exceeded, min exit slot %s, got %s",
          state.getSlot().plus(chainSpec.getEntryExitDelay()), validator.getExitSlot());
    }

    if (state.getSlot().less(exit.getSlot())) {
      return failedResult(
          "exit.slot must be greater or equal to state.slot, exit.slot=%s and state.slot=%s",
          exit.getSlot(), state.getSlot());
    }

    if (!specHelpers.bls_verify(
        validator.getPubKey(),
        Hash32.ZERO,
        exit.getSignature(),
        specHelpers.get_domain(state.getForkData(), exit.getSlot(), EXIT))) {
      return failedResult("failed to verify signature");
    }

    return PASSED;
  }
}
