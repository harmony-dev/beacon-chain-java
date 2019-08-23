package org.ethereum.beacon.chain.pool;

import java.time.Duration;
import org.ethereum.beacon.core.types.EpochNumber;
import org.reactivestreams.Publisher;

public interface AttestationPool {

  int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  EpochNumber MAX_ATTESTATION_LOOKAHEAD = EpochNumber.of(1);

  int MAX_KNOWN_ATTESTATIONS = 1_000_000;

  int UNKNOWN_BLOCK_POOL_SIZE = 100_000;

  int VERIFIER_BUFFER_SIZE = 10_000;

  Duration VERIFIER_INTERVAL = Duration.ofMillis(50);

  Publisher<ReceivedAttestation> valid();

  Publisher<ReceivedAttestation> invalid();

  Publisher<ReceivedAttestation> unknownBlock();

  Publisher<OffChainAggregates> aggregates();

  void start();
}
