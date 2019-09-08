package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.churn.AttestationChurn;
import org.ethereum.beacon.chain.pool.churn.AttestationChurnImpl;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;

public interface ObservableStateProcessor {
  void start();

  Publisher<ObservableBeaconState> getObservableStateStream();

  static ObservableStateProcessor createNew(
      BeaconChainSpec spec,
      EmptySlotTransition emptySlotTransition,
      Schedulers schedulers,
      Publisher<SlotNumber> newSlots,
      Publisher<BeaconChainHead> chainHeads,
      Publisher<Checkpoint> justifiedCheckpoints,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<Attestation> validAttestations) {
    AttestationChurn churn = new AttestationChurnImpl(spec, AttestationPool.ATTESTATION_CHURN_SIZE);
    return new YetAnotherStateProcessor(
        spec,
        emptySlotTransition,
        churn,
        schedulers,
        newSlots,
        chainHeads,
        justifiedCheckpoints,
        finalizedCheckpoints,
        validAttestations);
  }
}
