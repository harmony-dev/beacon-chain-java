package org.ethereum.beacon.chain.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationDequeued;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class NoBlockRootAttestationQueueImpl implements NoBlockRootAttestationQueue {

  private final Map<Hash32, Set<Attestation>> attestations = new HashMap<>();

  private final EventBus eventBus;
  private final BeaconChainSpec spec;

  public NoBlockRootAttestationQueueImpl(EventBus eventBus, BeaconChainSpec spec) {
    this.eventBus = eventBus;
    this.spec = spec;
  }

  @Override
  public void onBlock(BeaconBlock block) {
    Set<Attestation> bucket = attestations.remove(spec.signing_root(block));
    if (bucket != null) {
      bucket.forEach(attestation -> eventBus.publish(AttestationDequeued.wrap(attestation)));
    }
  }

  @Override
  public void onAttestation(Attestation attestation) {
    Set<Attestation> bucket =
        attestations.computeIfAbsent(
            attestation.getData().getBeaconBlockRoot(), root -> new HashSet<>());
    bucket.add(attestation);
  }
}
