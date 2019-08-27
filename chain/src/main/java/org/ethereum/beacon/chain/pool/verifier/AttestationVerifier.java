package org.ethereum.beacon.chain.pool.verifier;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import com.google.common.base.Objects;
import java.util.ArrayList;
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
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class AttestationVerifier implements BatchVerifier {

  private final BeaconTupleStorage tupleStorage;
  private final BeaconChainSpec spec;
  private final EmptySlotTransition emptySlotTransition;

  public AttestationVerifier(
      BeaconTupleStorage tupleStorage,
      BeaconChainSpec spec,
      EmptySlotTransition emptySlotTransition) {
    this.tupleStorage = tupleStorage;
    this.spec = spec;
    this.emptySlotTransition = emptySlotTransition;
  }

  @Override
  public VerificationResult verify(List<ReceivedAttestation> batch) {
    Map<AttestingTarget, List<ReceivedAttestation>> targetGroups =
        batch.stream().collect(Collectors.groupingBy(AttestingTarget::from));

    return targetGroups.entrySet().stream()
        .map(e -> verifyGroup(e.getKey(), e.getValue()))
        .reduce(VerificationResult.EMPTY, VerificationResult::merge);
  }

  private VerificationResult verifyGroup(AttestingTarget target, List<ReceivedAttestation> group) {
    Optional<BeaconTuple> rootTuple = tupleStorage.get(target.blockRoot);

    // it must be present, otherwise, attestation couldn't be here
    // TODO keep assertion for a while, it might be useful to discover bugs
    assert rootTuple.isPresent();

    EpochNumber beaconBlockEpoch = spec.compute_epoch_of_slot(rootTuple.get().getState().getSlot());

    // beaconBlockEpoch > targetEpoch
    // it must either be equal or less than
    if (beaconBlockEpoch.greater(target.checkpoint.getEpoch())) {
      return VerificationResult.allInvalid(group);
    }

    // beaconBlockEpoch < targetEpoch && targetRoot != beaconBlockRoot
    // target checkpoint is built with empty slots upon a block root
    // in that case target root and beacon block root must be equal
    if (beaconBlockEpoch.less(target.checkpoint.getEpoch())
        && !target.checkpoint.getRoot().equals(target.blockRoot)) {
      return VerificationResult.allInvalid(group);
    }

    // compute state and domain, there must be the same state for all attestations
    final BeaconState state = computeState(rootTuple.get(), target.checkpoint.getEpoch());
    final UInt64 domain = spec.get_domain(state, ATTESTATION, target.checkpoint.getEpoch());
    final AggregateSignatureVerifier signatureVerifier =
        new AggregateSignatureVerifier(spec, domain);
    final List<ReceivedAttestation> invalid = new ArrayList<>();

    for (ReceivedAttestation attestation : group) {
      Optional<IndexedAttestation> result = verifyIndexed(state, attestation.getMessage());
      if (result.isPresent()) {
        signatureVerifier.feed(state, result.get(), attestation);
      } else {
        invalid.add(attestation);
      }
    }

    VerificationResult signatureResult = signatureVerifier.verify();
    return VerificationResult.allInvalid(invalid).merge(signatureResult);
  }

  private Optional<IndexedAttestation> verifyIndexed(BeaconState state, Attestation attestation) {
    // skip indexed attestation verification, it's explicitly done in the next step
    if (!spec.verify_attestation_impl(state, attestation, false)) {
      return Optional.empty();
    }

    // compute and verify indexed attestation
    // skip signature verification
    IndexedAttestation indexedAttestation = spec.get_indexed_attestation(state, attestation);
    if (!spec.is_valid_indexed_attestation_impl(state, indexedAttestation, false)) {
      return Optional.empty();
    }

    return Optional.of(indexedAttestation);
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

  private static final class AttestingTarget {

    static AttestingTarget from(ReceivedAttestation attestation) {
      AttestationData data = attestation.getMessage().getData();
      return new AttestingTarget(data.getTarget(), data.getBeaconBlockRoot());
    }

    private final Checkpoint checkpoint;
    private final Hash32 blockRoot;

    private AttestingTarget(Checkpoint checkpoint, Hash32 blockRoot) {
      this.checkpoint = checkpoint;
      this.blockRoot = blockRoot;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AttestingTarget that = (AttestingTarget) o;
      return Objects.equal(checkpoint, that.checkpoint) && Objects.equal(blockRoot, that.blockRoot);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(checkpoint, blockRoot);
    }
  }
}
