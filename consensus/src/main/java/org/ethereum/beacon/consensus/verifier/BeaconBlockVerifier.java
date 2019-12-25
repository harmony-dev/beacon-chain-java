package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.block.AttestationListVerifier;
import org.ethereum.beacon.consensus.verifier.block.AttesterSlashingListVerifier;
import org.ethereum.beacon.consensus.verifier.block.DepositListVerifier;
import org.ethereum.beacon.consensus.verifier.block.VoluntaryExitListVerifier;
import org.ethereum.beacon.consensus.verifier.block.BlockHeaderVerifier;
import org.ethereum.beacon.consensus.verifier.block.ProposerSlashingListVerifier;
import org.ethereum.beacon.consensus.verifier.block.RandaoVerifier;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.consensus.verifier.operation.AttesterSlashingVerifier;
import org.ethereum.beacon.consensus.verifier.operation.DepositVerifier;
import org.ethereum.beacon.consensus.verifier.operation.VoluntaryExitVerifier;
import org.ethereum.beacon.consensus.verifier.operation.ProposerSlashingVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

/** A common interface for various {@link BeaconBlock} verifications defined by the spec. */
public interface BeaconBlockVerifier {

  static BeaconBlockVerifier createDefault(BeaconChainSpec spec) {
    return CompositeBlockVerifier.Builder.createNew()
        .with(new RandaoVerifier(spec))
        .with(new BlockHeaderVerifier(spec))
        .with(new ProposerSlashingListVerifier(new ProposerSlashingVerifier(spec), spec.getConstants()))
        .with(new AttesterSlashingListVerifier(new AttesterSlashingVerifier(spec), spec.getConstants()))
        .with(new AttestationListVerifier(new AttestationVerifier(spec), spec.getConstants()))
        .with(new DepositListVerifier(new DepositVerifier(spec), spec.getConstants()))
        .with(new VoluntaryExitListVerifier(new VoluntaryExitVerifier(spec), spec.getConstants()))
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
  VerificationResult verify(SignedBeaconBlock block, BeaconState state);
}
