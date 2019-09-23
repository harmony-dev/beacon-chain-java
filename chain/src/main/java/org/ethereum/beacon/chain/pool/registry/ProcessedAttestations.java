package org.ethereum.beacon.chain.pool.registry;

import java.util.function.Function;
import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A registry that stores relatively big number of attestation hashes.
 *
 * <p>This particular implementation is based on LRU map.
 *
 * <p>It's recommended to place this registry prior to highly resource demanded processors in order
 * to prevent double work.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
public class ProcessedAttestations implements AttestationRegistry {

  /** An entry of the map. */
  private static final Object ENTRY = new Object();
  /** LRU attestation cache. */
  private final LRUMap<Hash32, Object> cache;
  /** A function that given attestation returns its hash. */
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
