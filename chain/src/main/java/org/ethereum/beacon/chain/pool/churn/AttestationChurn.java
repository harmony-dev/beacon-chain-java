package org.ethereum.beacon.chain.pool.churn;

import java.util.List;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.chain.pool.StatefulProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;

public interface AttestationChurn extends StatefulProcessor {

  OffChainAggregates compute(BeaconTuple tuple);

  void add(List<Attestation> attestation);

  void feedFinalizedCheckpoint(Checkpoint checkpoint);

  void feedJustifiedCheckpoint(Checkpoint checkpoint);

  void feedNewSlot(SlotNumber slotNumber);

  static AttestationChurn create(BeaconChainSpec spec, long size) {
    return new AttestationChurnImpl(spec, size);
  }
}
