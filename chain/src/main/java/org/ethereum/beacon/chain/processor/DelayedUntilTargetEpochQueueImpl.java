package org.ethereum.beacon.chain.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationDequeued;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedUntilTargetEpochQueueImpl implements DelayedUntilTargetEpochQueue {

  private final Map<SlotNumber, Set<Attestation>> attestations = new HashMap<>();

  private final EventBus eventBus;
  private final BeaconChainSpec spec;

  public DelayedUntilTargetEpochQueueImpl(EventBus eventBus, BeaconChainSpec spec) {
    this.eventBus = eventBus;
    this.spec = spec;
  }

  @Override
  public void onTick(SlotNumber slot) {
    Set<Attestation> bucket = attestations.remove(slot);
    if (bucket != null) {
      bucket.forEach(attestation -> eventBus.publish(AttestationDequeued.wrap(attestation)));
    }
  }

  @Override
  public void onAttestation(Attestation attestation) {
    Set<Attestation> bucket =
        attestations.computeIfAbsent(
            spec.compute_start_slot_at_epoch(attestation.getData().getTarget().getEpoch()),
            slot -> new HashSet<>());
    bucket.add(attestation);
  }
}
