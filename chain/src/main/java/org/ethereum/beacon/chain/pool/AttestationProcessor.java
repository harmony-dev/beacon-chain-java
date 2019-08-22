package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;

public interface AttestationProcessor {

  Publisher<ReceivedAttestation> out();

  void in(ReceivedAttestation attestation);
}
