package org.ethereum.beacon.chain.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationBatchDequeued;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedAttestationQueueImpl implements DelayedAttestationQueue {

  private final EventBus eventBus;

  private final TreeMap<SlotNumber, Set<Attestation>> attestations = new TreeMap<>();

  public DelayedAttestationQueueImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, Set<Attestation>> slotBucket = attestations.headMap(slot, false);
    Set<Attestation> dequeuedAttestations =
        slotBucket.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    eventBus.publish(AttestationBatchDequeued.wrap(dequeuedAttestations));
    attestations.keySet().removeAll(slotBucket.keySet());
  }

  @Override
  public void onAttestation(Attestation attestation) {
    Set<Attestation> slotBucket =
        attestations.computeIfAbsent(attestation.getData().getSlot(), slot -> new HashSet<>());
    slotBucket.add(attestation);
  }
}
