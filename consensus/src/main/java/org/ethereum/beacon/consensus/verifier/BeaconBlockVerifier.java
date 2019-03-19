package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.block.AttestationListVerifier;
import org.ethereum.beacon.consensus.verifier.block.DepositListVerifier;
import org.ethereum.beacon.consensus.verifier.block.TransferListVerifier;
import org.ethereum.beacon.consensus.verifier.block.VoluntaryExitListVerifier;
import org.ethereum.beacon.consensus.verifier.block.BlockSignatureVerifier;
import org.ethereum.beacon.consensus.verifier.block.ProposerSlashingListVerifier;
import org.ethereum.beacon.consensus.verifier.block.RandaoVerifier;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.consensus.verifier.operation.DepositVerifier;
import org.ethereum.beacon.consensus.verifier.operation.TransferVerifier;
import org.ethereum.beacon.consensus.verifier.operation.VoluntaryExitVerifier;
import org.ethereum.beacon.consensus.verifier.operation.ProposerSlashingVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/** A common interface for various {@link BeaconBlock} verifications defined by the spec. */
public interface BeaconBlockVerifier {

  static BeaconBlockVerifier createDefault(SpecHelpers specHelpers) {
    return CompositeBlockVerifier.Builder.createNew()
        .with(new RandaoVerifier(specHelpers))
        .with(new BlockSignatureVerifier(specHelpers))
        .with(new AttestationListVerifier(new AttestationVerifier(specHelpers), specHelpers.getConstants()))
        .with(new DepositListVerifier(new DepositVerifier(specHelpers), specHelpers.getConstants()))
        .with(new VoluntaryExitListVerifier(new VoluntaryExitVerifier(specHelpers), specHelpers.getConstants()))
        .with(new ProposerSlashingListVerifier(new ProposerSlashingVerifier(specHelpers), specHelpers.getConstants()))
        .with(new TransferListVerifier(new TransferVerifier(specHelpers), specHelpers))
        .build();
  }

  /**
   * Runs block verifications.
   *
   * @param block a block to verify.
   * @param state a state which slot number is equal to {@code block.getSlot()} produced by per-slot
   *     processing.
   * @return result of the verifications.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
   *     processing</a> in the spec.
   */
  VerificationResult verify(BeaconBlock block, BeaconState state);
}
