package org.ethereum.beacon.chain.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedAttestationQueueImpl implements DelayedAttestationQueue {

  private final TreeMap<SlotNumber, List<Attestation>> attestations = new TreeMap<>();

  private Consumer<Attestation> subscriber;

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, List<Attestation>> slotBucket = attestations.headMap(slot, false);
    if (subscriber != null) {
      slotBucket.values().forEach(bucket -> bucket.forEach(subscriber));
    }
    attestations.keySet().removeAll(slotBucket.keySet());
  }

  @Override
  public void onAttestation(Attestation attestation) {
    List<Attestation> slotBucket =
        attestations.computeIfAbsent(attestation.getData().getSlot(), slot -> new ArrayList<>());
    slotBucket.add(attestation);
  }

  @Override
  public void subscribe(Consumer<Attestation> subscriber) {
    this.subscriber = subscriber;
  }
}
