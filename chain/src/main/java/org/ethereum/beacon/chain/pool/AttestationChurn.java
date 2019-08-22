package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;

public interface AttestationChurn {

  Publisher<OffChainAggregates> out();

  void in(ReceivedAttestation attestation);
}
