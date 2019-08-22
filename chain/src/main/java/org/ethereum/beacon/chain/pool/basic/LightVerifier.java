package org.ethereum.beacon.chain.pool.basic;

import com.google.common.annotations.VisibleForTesting;
import org.ethereum.beacon.chain.pool.AbstractVerifier;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.AttestationVerifier;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class LightVerifier extends AbstractVerifier implements AttestationVerifier {

  private final BeaconChainSpec spec;

  private Checkpoint finalizedCheckpoint;
  private EpochNumber maxAcceptableEpoch;

  public LightVerifier(
      Schedulers schedulers,
      Publisher<Checkpoint> finalizedCheckpoint,
      Publisher<SlotNumber> slotClock,
      BeaconChainSpec spec) {
    super(schedulers, "BasicVerifier");

    this.spec = spec;
    Flux.from(finalizedCheckpoint).subscribe(this::onNewFinalizedCheckpoint);
    Flux.from(slotClock).subscribe(this::onNewSlot);
  }

  @Override
  public void in(ReceivedAttestation attestation) {
    if (!isInitialized()) {
      return;
    }

    if (isValid(attestation)) {
      valid.onNext(attestation);
    } else {
      invalid.onNext(attestation);
    }
  }

  private boolean isValid(ReceivedAttestation attestation) {
    final AttestationData data = attestation.getMessage().getData();
    final Checkpoint finalizedCheckpoint = this.finalizedCheckpoint;
    final EpochNumber maxAcceptableEpoch = this.maxAcceptableEpoch;

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

  @VisibleForTesting
  void onNewFinalizedCheckpoint(Checkpoint checkpoint) {
    this.finalizedCheckpoint = checkpoint;
  }

  @VisibleForTesting
  void onNewSlot(SlotNumber slot) {
    this.maxAcceptableEpoch =
        spec.compute_epoch_of_slot(slot).plus(AttestationPool.MAX_ATTESTATION_LOOKAHEAD);
  }

  @VisibleForTesting
  boolean isInitialized() {
    return finalizedCheckpoint != null && maxAcceptableEpoch != null;
  }

  @Override
  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }
}
