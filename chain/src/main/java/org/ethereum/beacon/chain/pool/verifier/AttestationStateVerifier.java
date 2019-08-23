package org.ethereum.beacon.chain.pool.verifier;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class AttestationStateVerifier {

  private final Scheduler executor;
  private final BeaconTupleStorage tupleStorage;
  private final BeaconChainSpec spec;
  private final EmptySlotTransition emptySlotTransition;

  private final SimpleProcessor<SignatureVerificationSet> valid;
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

    this.valid = new SimpleProcessor<>(schedulers.events(), "AttestationStateVerifier.valid");
    this.invalid = new SimpleProcessor<>(schedulers.events(), "AttestationStateVerifier.invalid");
  }

  public Publisher<SignatureVerificationSet> valid() {
    return valid;
  }

  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }

  public void in(List<ReceivedAttestation> batch) {
    execute(
        () -> {
          Map<Pair<Checkpoint, Hash32>, List<ReceivedAttestation>> groupedByState =
              batch.stream()
                  .collect(
                      Collectors.groupingBy(
                          attestation ->
                              Pair.with(
                                  attestation.getMessage().getData().getTarget(),
                                  attestation.getMessage().getData().getBeaconBlockRoot())));

          groupedByState.forEach((key, value) -> process(key.getValue0(), key.getValue1(), value));
        });
  }

  private void process(
      final Checkpoint target,
      final Hash32 beaconBlockRoot,
      List<ReceivedAttestation> attestations) {
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

    // compute state and domain, there must be the same state for all attestations
    final BeaconState state = computeState(rootTuple.get(), target.getEpoch());
    final UInt64 domain = spec.get_domain(state, ATTESTATION, target.getEpoch());
    for (ReceivedAttestation attestation : attestations) {
      // skip signature verification, it's passed on the next processor
      if (!spec.verify_attestation_impl(state, attestation.getMessage(), false)) {
        invalid.onNext(attestation);
        continue;
      }

      // compute and verify indexed attestation
      IndexedAttestation indexedAttestation =
          spec.get_indexed_attestation(state, attestation.getMessage());
      if (!spec.is_valid_indexed_attestation_impl(state, indexedAttestation, false)) {
        invalid.onNext(attestation);
        continue;
      }

      // compute data required for signature verification
      List<BLSPubkey> bit0Keys =
          indexedAttestation.getCustodyBit0Indices().stream()
              .map(i -> state.getValidators().get(i).getPubKey())
              .collect(Collectors.toList());
      List<BLSPubkey> bit1Keys =
          indexedAttestation.getCustodyBit1Indices().stream()
              .map(i -> state.getValidators().get(i).getPubKey())
              .collect(Collectors.toList());

      // send them to signature verifier
      valid.onNext(
          new SignatureVerificationSet(
              spec.bls_aggregate_pubkeys_no_validate(bit0Keys),
              spec.bls_aggregate_pubkeys_no_validate(bit1Keys),
              domain,
              attestation));
    }
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

  private void execute(RunnableEx routine) {
    executor.execute(routine);
  }
}
