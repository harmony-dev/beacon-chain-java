package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.StatefulProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;

/**
 * Given attestation runs a number of sanity checks against it.
 *
 * <p>This is one of the first processors in attestation pool pipeline. Attestations that are not
 * passing these checks SHOULD be considered invalid.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
public class SanityChecker implements AttestationChecker, StatefulProcessor {

  /** A beacon chain spec. */
  private final BeaconChainSpec spec;
  /** Most recent finalized checkpoint. */
  private Checkpoint finalizedCheckpoint;

  public SanityChecker(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public boolean check(ReceivedAttestation attestation) {
    assert isInitialized();

    final AttestationData data = attestation.getMessage().getData();

    // sourceEpoch >= targetEpoch
    if (data.getSource().getEpoch().greaterEqual(data.getTarget().getEpoch())) {
      return false;
    }

    // finalizedEpoch == sourceEpoch && finalizedRoot != sourceRoot
    if (data.getSource().getEpoch().equals(finalizedCheckpoint.getEpoch())
        && !finalizedCheckpoint.getRoot().equals(data.getSource().getRoot())) {
      return false;
    }

    // crosslinkShard >= SHARD_COUNT
    if (data.getCrosslink().getShard().greaterEqual(spec.getConstants().getShardCount())) {
      return false;
    }

    return true;
  }

  /**
   * Update the most recent finalized checkpoint.
   *
   * <p>This method should be called each time new finalized checkpoint appears.
   *
   * @param checkpoint finalized checkpoint.
   */
  public void feedFinalizedCheckpoint(Checkpoint checkpoint) {
    this.finalizedCheckpoint = checkpoint;
  }

  @Override
  public boolean isInitialized() {
    return finalizedCheckpoint != null;
  }
}
