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
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An implementation of {@link BatchVerifier}.
 *
 * <p>There are three steps of batch verification:
 *
 * <ul>
 *   <li>Group batch by beacon block root and target.
 *   <li>Calculate state for each group and run checks against this state.
 *   <li>Pass group onto signature verifier.
 * </ul>
 *
 * <p>Current implementation relies on {@link AggregateSignatureVerifier} which is pretty efficient.
 * {@link AggregateSignatureVerifier} tries to first aggregate attestations and then verify a
 * signature of that aggregate in a single operation instead of verifying signature of each
 * standalone attestation. A group succeeded with state verification is next passed onto signature
 * verifier. A nice part of it is that aggregatable attestation groups are subsets of groups made
 * against attestation target.
 *
 * <p>Verification hierarchy can be represented with a diagram:
 *
 * <pre>
 *                        attestation_batch
 *                       /                 \
 * state:               target_1 ... target_N
 *                     /        \
 * signature:  aggregate_1 ... aggregate_N
 * </pre>
 */
public class AttestationVerifier implements BatchVerifier {

  /** A beacon tuple storage. */
  private final BeaconTupleStorage tupleStorage;
  /** A beacon chain spec. */
  private final BeaconChainSpec spec;
  /** An empty slot transition. */
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

  /**
   * Verifies a group of attestations with the same target.
   *
   * @param target a target.
   * @param group a group.
   * @return result of verification.
   */
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
    final BeaconState state =
        computeState(rootTuple.get().getState(), target.checkpoint.getEpoch());
    final UInt64 domain = spec.get_domain(state, ATTESTATION, target.checkpoint.getEpoch());
    final AggregateSignatureVerifier signatureVerifier =
        new AggregateSignatureVerifier(spec, domain);
    final List<ReceivedAttestation> invalid = new ArrayList<>();

    for (ReceivedAttestation attestation : group) {
      Optional<IndexedAttestation> result = verifyIndexed(state, attestation.getMessage());
      if (result.isPresent()) {
        signatureVerifier.add(state, result.get(), attestation);
      } else {
        invalid.add(attestation);
      }
    }

    VerificationResult signatureResult = signatureVerifier.verify();
    return VerificationResult.allInvalid(invalid).merge(signatureResult);
  }

  /**
   * This method does two things:
   *
   * <ul>
   *   <li>Runs main checks defined in the spec; these checks verifies attestation against a state
   *       it's been made upon.
   *   <li>Computes {@link IndexedAttestation} and runs checks against it omitting signature
   *       verification.
   * </ul>
   *
   * @param state a state attestation built upon.
   * @param attestation an attestation.
   * @return an optional filled with {@link IndexedAttestation} instance if verification passed
   *     successfully, empty optional box is returned otherwise.
   */
  private Optional<IndexedAttestation> verifyIndexed(BeaconState state, Attestation attestation) {
    // compute and verify indexed attestation
    // skip signature verification
    IndexedAttestation indexedAttestation = spec.get_indexed_attestation(state, attestation);
    if (!spec.is_valid_indexed_attestation_impl(state, indexedAttestation, false)) {
      return Optional.empty();
    }

    return Optional.of(indexedAttestation);
  }

  /**
   * Given epoch and beacon state computes a state that attestation built upon.
   *
   * @param state a state after attestation beacon block has been imported.
   * @param targetEpoch target epoch of attestation.
   * @return computed state.
   */
  private BeaconState computeState(BeaconStateEx state, EpochNumber targetEpoch) {
    EpochNumber beaconBlockEpoch = spec.compute_epoch_of_slot(state.getSlot());

    // block is in the same epoch, no additional state is required to be built
    if (beaconBlockEpoch.equals(targetEpoch)) {
      return state;
    }

    // build a state at epoch boundary, it must be enough to proceed
    return emptySlotTransition.apply(state, spec.compute_start_slot_of_epoch(targetEpoch));
  }

  /**
   * A wrapper for attestation target checkpoint and beacon block root.
   *
   * <p>This is the entity which initial verification groups are built around.
   */
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
