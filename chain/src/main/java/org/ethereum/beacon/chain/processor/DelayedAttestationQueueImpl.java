package org.ethereum.beacon.chain.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationUnparked;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedAttestationQueueImpl implements DelayedAttestationQueue {

  private final EventBus eventBus;

  private final TreeMap<SlotNumber, List<Attestation>> attestations = new TreeMap<>();

  public DelayedAttestationQueueImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, List<Attestation>> slotBucket = attestations.headMap(slot, false);
    slotBucket
        .values()
        .forEach(bucket -> bucket.forEach(a -> eventBus.publish(AttestationUnparked.wrap(a))));
    attestations.keySet().removeAll(slotBucket.keySet());
  }

  @Override
  public void onAttestation(Attestation attestation) {
    List<Attestation> slotBucket =
        attestations.computeIfAbsent(attestation.getData().getSlot(), slot -> new ArrayList<>());
    slotBucket.add(attestation);
  }
}
