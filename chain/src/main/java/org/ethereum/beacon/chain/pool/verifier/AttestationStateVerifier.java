package org.ethereum.beacon.chain.pool.verifier;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class AttestationStateVerifier {

  private final Queue queue = new Queue();
  private final Scheduler executor;
  private final BeaconTupleStorage tupleStorage;
  private final BeaconChainSpec spec;
  private final EmptySlotTransition emptySlotTransition;

  private final SimpleProcessor<Pair<BeaconState, List<ReceivedAttestation>>> outbound;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public AttestationStateVerifier(
      Schedulers schedulers,
      Scheduler executor,
      BeaconTupleStorage tupleStorage,
      BeaconChainSpec spec,
      EmptySlotTransition emptySlotTransition) {
    this.executor = executor;
    this.tupleStorage = tupleStorage;
    this.spec = spec;
    this.emptySlotTransition = emptySlotTransition;

    this.outbound = new SimpleProcessor<>(schedulers.events(), "AttestationStateVerifier.outbound");
    this.invalid = new SimpleProcessor<>(schedulers.events(), "AttestationStateVerifier.invalid");
  }

  public Publisher<Pair<BeaconState, List<ReceivedAttestation>>> outbound() {
    return outbound;
  }

  public void inbound(ReceivedAttestation attestation) {
    queue.add(attestation);
    execute(this::nudgeQueue);
  }

  private void nudgeQueue() {
    while (queue.size() > 0) {
      execute(
          () -> {
            List<ReceivedAttestation> batch = queue.take();
            process(batch);
          });
    }
  }

  private void execute(RunnableEx routine) {
    executor.execute(routine);
  }

  private void process(List<ReceivedAttestation> attestations) {
    if (attestations.isEmpty()) {
      return;
    }

    final Checkpoint target = attestations.get(0).getMessage().getData().getTarget();
    final Hash32 beaconBlockRoot = attestations.get(0).getMessage().getData().getBeaconBlockRoot();

    Optional<BeaconTuple> rootTuple = tupleStorage.get(beaconBlockRoot);

    // it must be present, otherwise, attestation couldn't be here
    // TODO keep assertion for a while, it might be useful to discover bugs
    assert rootTuple.isPresent();

    EpochNumber beaconBlockEpoch = spec.compute_epoch_of_slot(rootTuple.get().getState().getSlot());

    // beaconBlockEpoch > targetEpoch
    // it must either be equal or less than
    if (beaconBlockEpoch.greater(target.getEpoch())) {
      attestations.forEach(invalid::onNext);
      return;
    }

    // beaconBlockEpoch < targetEpoch && targetRoot != beaconBlockRoot
    // target checkpoint is built with empty slots upon a block root
    // in that case target root and beacon block root must be equal
    if (beaconBlockEpoch.less(target.getEpoch()) && !target.getRoot().equals(beaconBlockRoot)) {
      attestations.forEach(invalid::onNext);
      return;
    }

    // compute state, there must be the same state for all attestations
    final BeaconState state = computeState(rootTuple.get(), target.getEpoch());
    final List<ReceivedAttestation> validAttestations = new ArrayList<>();
    for (ReceivedAttestation attestation : attestations) {
      // skip signature verification, it's handled by next processor
      if (!spec.verify_attestation_impl(state, attestation.getMessage(), false)) {
        validAttestations.add(attestation);
      } else {
        invalid.onNext(attestation);
      }
    }

    outbound.onNext(Pair.with(state, validAttestations));
  }

  private BeaconState computeState(BeaconTuple rootTuple, EpochNumber targetEpoch) {
    EpochNumber beaconBlockEpoch = spec.compute_epoch_of_slot(rootTuple.getState().getSlot());

    // block is in the same epoch, no additional state is required to be built
    if (beaconBlockEpoch.equals(targetEpoch)) {
      return rootTuple.getState();
    }

    // build a state at epoch boundary, it must be enough to proceed
    return emptySlotTransition.apply(
        rootTuple.getState(), spec.compute_start_slot_of_epoch(targetEpoch));
  }

  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }

  private static final class StateTuple {
    private final Checkpoint target;
    private final Hash32 beaconBlockRoot;

    private StateTuple(Checkpoint target, Hash32 beaconBlockRoot) {
      this.target = target;
      this.beaconBlockRoot = beaconBlockRoot;
    }

    static StateTuple from(ReceivedAttestation attestation) {
      return new StateTuple(
          attestation.getMessage().getData().getTarget(),
          attestation.getMessage().getData().getBeaconBlockRoot());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StateTuple that = (StateTuple) o;
      return Objects.equal(target, that.target)
          && Objects.equal(beaconBlockRoot, that.beaconBlockRoot);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(target, beaconBlockRoot);
    }
  }

  private static final class Queue {

    private final LinkedHashMap<StateTuple, List<ReceivedAttestation>> queue =
        new LinkedHashMap<>();

    synchronized void add(ReceivedAttestation attestation) {
      List<ReceivedAttestation> bucket =
          queue.computeIfAbsent(StateTuple.from(attestation), key -> new ArrayList<>());
      bucket.add(attestation);
    }

    synchronized List<ReceivedAttestation> take() {
      Iterator<List<ReceivedAttestation>> it = queue.values().iterator();
      if (it.hasNext()) {
        List<ReceivedAttestation> ret = it.next();
        it.remove();
        return ret;
      } else {
        return Collections.emptyList();
      }
    }

    int size() {
      return queue.size();
    }
  }
}
