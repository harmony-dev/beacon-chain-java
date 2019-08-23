package org.ethereum.beacon.chain.pool.basic;

import java.util.function.Function;
import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.beacon.chain.pool.AbstractProcessor;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.AttestationProcessor;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.schedulers.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class ProcessedAttestationPool extends AbstractProcessor implements AttestationProcessor {

  private static final Object ENTRY = new Object();

  private final LRUMap<Hash32, Object> processedAttestationHash =
      new LRUMap<>(AttestationPool.MAX_KNOWN_ATTESTATIONS);
  private final Function<Attestation, Hash32> hasher;

  public ProcessedAttestationPool(Schedulers schedulers, Function<Attestation, Hash32> hasher) {
    super(schedulers, "ProcessedAttestationPool");
    this.hasher = hasher;
  }

  @Override
  public void in(ReceivedAttestation attestation) {
    Object existed = processedAttestationHash.put(hasher.apply(attestation.getMessage()), ENTRY);
    if (existed == null) {
      outbound.onNext(attestation);
    }
  }
}
