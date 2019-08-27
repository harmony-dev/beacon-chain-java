package org.ethereum.beacon.chain.pool.registry;

import java.util.function.Function;
import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class ProcessedAttestations implements AttestationRegistry {
  private static final Object ENTRY = new Object();
  private final LRUMap<Hash32, Object> cache;
  private final Function<Attestation, Hash32> hasher;

  public ProcessedAttestations(Function<Attestation, Hash32> hasher, int size) {
    this.hasher = hasher;
    this.cache = new LRUMap<>(size);
  }

  @Override
  public boolean add(ReceivedAttestation attestation) {
    return null == cache.put(hasher.apply(attestation.getMessage()), ENTRY);
  }
}
