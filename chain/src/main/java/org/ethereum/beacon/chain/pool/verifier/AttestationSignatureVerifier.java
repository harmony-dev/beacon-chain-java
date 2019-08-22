package org.ethereum.beacon.chain.pool.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;

public class AttestationSignatureVerifier {

  private final Queue queue = new Queue();
  private final Scheduler executor;
  private final BeaconChainSpec spec;

  private final SimpleProcessor<ReceivedAttestation> valid;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public AttestationSignatureVerifier(
      Schedulers schedulers, Scheduler executor, BeaconChainSpec spec) {
    this.executor = executor;
    this.spec = spec;

    this.valid =
        new SimpleProcessor<>(schedulers.events(), "AttestationSignatureVerifier.valid");
    this.invalid =
        new SimpleProcessor<>(schedulers.events(), "AttestationSignatureVerifier.invalid");
  }

  public Publisher<ReceivedAttestation> valid() {
    return valid;
  }

  public void inbound(Pair<BeaconState, List<ReceivedAttestation>> attestationTuple) {
    queue.add(attestationTuple);
    execute(this::nudgeQueue);
  }

  private void nudgeQueue() {
    while (queue.size() > 0) {
      execute(
          () -> {
            Pair<BeaconState, List<ReceivedAttestation>> batch = queue.take();
            process(batch);
          });
    }
  }

  private void execute(RunnableEx routine) {
    executor.execute(routine);
  }

  private void process(Pair<BeaconState, List<ReceivedAttestation>> attestationTuple) {
    if (attestationTuple.getValue1().isEmpty()) {
      return;
    }

    final BeaconState state = attestationTuple.getValue0();
    for (ReceivedAttestation attestation : attestationTuple.getValue1()) {
      IndexedAttestation indexedAttestation =
          spec.get_indexed_attestation(state, attestation.getMessage());
      if (spec.is_valid_indexed_attestation(state, indexedAttestation)) {
        valid.onNext(attestation);
      } else {
        invalid.onNext(attestation);
      }
    }
  }

  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }

  private static final class Queue {

    private final LinkedHashMap<Checkpoint, Pair<BeaconState, List<ReceivedAttestation>>> queue =
        new LinkedHashMap<>();

    synchronized void add(Pair<BeaconState, List<ReceivedAttestation>> attestationTuple) {
      if (attestationTuple.getValue1().isEmpty()) {
        return;
      }

      Pair<BeaconState, List<ReceivedAttestation>> bucket =
          queue.computeIfAbsent(
              attestationTuple.getValue1().get(0).getMessage().getData().getTarget(),
              key -> Pair.with(attestationTuple.getValue0(), new ArrayList<>()));
      bucket.getValue1().addAll(attestationTuple.getValue1());
    }

    synchronized Pair<BeaconState, List<ReceivedAttestation>> take() {
      Iterator<Pair<BeaconState, List<ReceivedAttestation>>> it = queue.values().iterator();
      if (it.hasNext()) {
        Pair<BeaconState, List<ReceivedAttestation>> ret = it.next();
        it.remove();
        return ret;
      } else {
        return Pair.with(BeaconState.getEmpty(), Collections.emptyList());
      }
    }

    int size() {
      return queue.size();
    }
  }
}
