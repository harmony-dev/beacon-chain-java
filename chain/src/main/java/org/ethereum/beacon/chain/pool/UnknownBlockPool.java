package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;

public interface UnknownBlockPool extends AttestationProcessor {

  Publisher<ReceivedAttestation> unknownBlock();
}
