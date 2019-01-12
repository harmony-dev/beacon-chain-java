package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import java.util.List;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies beacon operations that are held by {@link BeaconBlockBody}.
 *
 * @see OperationVerifier
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#operations">Operations</a>
 *     in the spec.
 */
public class BeaconOperationsVerifier implements BeaconBlockVerifier {

  private ChainSpec chainSpec;
  private OperationVerifier<ProposerSlashing> proposerSlashingVerifier;
  private OperationVerifier<CasperSlashing> casperSlashingVerifier;
  private OperationVerifier<Attestation> attestationVerifier;
  private OperationVerifier<Deposit> depositVerifier;
  private OperationVerifier<Exit> exitVerifier;

  public BeaconOperationsVerifier(
      ChainSpec chainSpec,
      OperationVerifier<ProposerSlashing> proposerSlashingVerifier,
      OperationVerifier<CasperSlashing> casperSlashingVerifier,
      OperationVerifier<Attestation> attestationVerifier,
      OperationVerifier<Deposit> depositVerifier,
      OperationVerifier<Exit> exitVerifier) {
    this.chainSpec = chainSpec;
    this.proposerSlashingVerifier = proposerSlashingVerifier;
    this.casperSlashingVerifier = casperSlashingVerifier;
    this.attestationVerifier = attestationVerifier;
    this.depositVerifier = depositVerifier;
    this.exitVerifier = exitVerifier;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    BeaconBlockBody body = block.getBody();

    if (body.getProposerSlashings().size() > chainSpec.getMaxProposerSlashings()) {
      return VerificationResult.createdFailed(
          "Block exceeds proposer slashings number, should be at most %d but got %d, block %s",
          chainSpec.getMaxProposerSlashings(), body.getProposerSlashings().size(), block);
    }

    if (body.getCasperSlashings().size() > chainSpec.getMaxCasperSlashings()) {
      return VerificationResult.createdFailed(
          "Block exceeds casper slashings number, should be at most %d but got %d, block %s",
          chainSpec.getMaxCasperSlashings(), body.getCasperSlashings().size(), block);
    }

    if (body.getAttestations().size() > chainSpec.getMaxAttestations()) {
      return VerificationResult.createdFailed(
          "Block exceeds attestations number, should be at most %d but got %d, block %s",
          chainSpec.getMaxAttestations(), body.getAttestations().size(), block);
    }

    if (body.getDeposits().size() > chainSpec.getMaxDeposits()) {
      return VerificationResult.createdFailed(
          "Block exceeds deposits number, should be at most %d but got %d, block %s",
          chainSpec.getMaxDeposits(), body.getDeposits().size(), block);
    }

    if (body.getExits().size() > chainSpec.getMaxExits()) {
      return VerificationResult.createdFailed(
          "Block exceeds exits number, should be at most %d but got %d, block %s",
          chainSpec.getMaxExits(), body.getExits().size(), block);
    }

    if (!body.getCustodyChallenges().isEmpty()) {
      return VerificationResult.createdFailed(
          "Block exceeds custody challenges number, should be at most %d but got %d, block %s",
          0, body.getCustodyChallenges().size(), block);
    }

    if (!body.getCustodyResponses().isEmpty()) {
      return VerificationResult.createdFailed(
          "Block exceeds custody responses number, should be at most %d but got %d, block %s",
          0, body.getCustodyResponses().size(), block);
    }

    if (!body.getCustodyReseeds().isEmpty()) {
      return VerificationResult.createdFailed(
          "Block exceeds custody reseeds number, should be at most %d but got %d, block %s",
          0, body.getCustodyReseeds().size(), block);
    }

    VerificationResult result;
    if (PASSED
        != (result = verifyList(proposerSlashingVerifier, body.getProposerSlashings(), state))) {
      return result;
    }

    if (PASSED != (result = verifyList(casperSlashingVerifier, body.getCasperSlashings(), state))) {
      return result;
    }

    if (PASSED != (result = verifyList(attestationVerifier, body.getAttestations(), state))) {
      return result;
    }

    if (PASSED != (result = verifyList(depositVerifier, body.getDeposits(), state))) {
      return result;
    }

    if (PASSED != (result = verifyList(exitVerifier, body.getExits(), state))) {
      return result;
    }

    return PASSED;
  }

  private <T> VerificationResult verifyList(
      OperationVerifier<T> verifier, List<T> operations, BeaconState state) {
    for (T operation : operations) {
      VerificationResult result = verifier.verify(operation, state);
      if (result != PASSED) {
        return result;
      }
    }
    return PASSED;
  }
}
