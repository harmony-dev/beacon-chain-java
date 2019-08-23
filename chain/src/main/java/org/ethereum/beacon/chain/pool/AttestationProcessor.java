package org.ethereum.beacon.chain.pool;

import java.util.List;
import org.reactivestreams.Publisher;

public interface AttestationProcessor {

  Publisher<ReceivedAttestation> out();

  void in(ReceivedAttestation attestation);

  default void batchIn(List<ReceivedAttestation> batch) {
    batch.forEach(this::in);
  }
}
