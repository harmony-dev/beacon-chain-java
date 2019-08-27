package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.StatefulProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;

public class SanityChecker implements AttestationChecker, StatefulProcessor {

  private final BeaconChainSpec spec;

  private Checkpoint finalizedCheckpoint;
  private EpochNumber maxAcceptableEpoch;

  public SanityChecker(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public boolean check(ReceivedAttestation attestation) {
    assert isStateReady();

    final AttestationData data = attestation.getMessage().getData();

    // sourceEpoch >= targetEpoch
    if (data.getSource().getEpoch().greaterEqual(data.getTarget().getEpoch())) {
      return false;
    }

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

  public void feedFinalizedCheckpoint(Checkpoint checkpoint) {
    this.finalizedCheckpoint = checkpoint;
  }

  public void feedNewSlot(SlotNumber newSlot) {
    this.maxAcceptableEpoch =
        spec.compute_epoch_of_slot(newSlot).plus(AttestationPool.MAX_ATTESTATION_LOOKAHEAD);
  }

  @Override
  public boolean isStateReady() {
    return finalizedCheckpoint != null && maxAcceptableEpoch != null;
  }
}
