package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.*;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.*;

/**
 * Filters attestations by time frame of its target and source.
 *
 * <p>Attestations not passing these checks SHOULD NOT be considered as invalid, they rather SHOULD
 * be considered as those which client is not interested in. Filtered out attestations SHOULD merely
 * be discarded.
 *
 * <p>This is the first filter in attestation processors pipeline.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
public class TimeFrameFilter implements AttestationChecker, StatefulProcessor {

  /** A beacon chain spec. */
  private final BeaconChainSpec spec;
  /** Accept attestations not older than current epoch plus this number. */
  private final EpochNumber maxAttestationLookahead;

  /** Most recent finalized checkpoint. */
  private Checkpoint finalizedCheckpoint;
  /** Upper time frame boundary. */
  private EpochNumber maxAcceptableEpoch;

  public TimeFrameFilter(BeaconChainSpec spec, EpochNumber maxAttestationLookahead) {
    this.spec = spec;
    this.maxAttestationLookahead = maxAttestationLookahead;
  }

  @Override
  public boolean isInitialized() {
    return finalizedCheckpoint != null && maxAcceptableEpoch != null;
  }

  @Override
  public boolean check(ReceivedAttestation attestation) {
    assert isInitialized();

    final AttestationData data = attestation.getMessage().getData();

    // targetEpoch <= finalizedEpoch
    if (data.getTarget().getEpoch().lessEqual(finalizedCheckpoint.getEpoch())) {
      return false;
    }

    // sourceEpoch < finalizedEpoch
    if (data.getSource().getEpoch().lessEqual(finalizedCheckpoint.getEpoch())) {
      return false;
    }

    // targetEpoch > maxAcceptableEpoch
    if (data.getTarget().getEpoch().greater(maxAcceptableEpoch)) {
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

  /**
   * This method should be called on each new slot.
   *
   * @param newSlot a new slot.
   */
  public void feedNewSlot(SlotNumber newSlot) {
    this.maxAcceptableEpoch = spec.compute_epoch_of_slot(newSlot).plus(maxAttestationLookahead);
  }
}
