package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;

public interface AttestationVerifier extends AttestationProcessor {

  default Publisher<ReceivedAttestation> out() {
    return valid();
  }

  Publisher<ReceivedAttestation> valid();

  Publisher<ReceivedAttestation> invalid();

}
