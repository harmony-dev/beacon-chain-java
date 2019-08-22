package org.ethereum.beacon.chain.pool.churn;

import org.ethereum.beacon.chain.pool.AttestationChurn;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.reactivestreams.Publisher;

public class AttestationChurnImpl implements AttestationChurn {

  @Override
  public Publisher<OffChainAggregates> out() {
    return null;
  }

  @Override
  public void in(ReceivedAttestation attestation) {}
}
