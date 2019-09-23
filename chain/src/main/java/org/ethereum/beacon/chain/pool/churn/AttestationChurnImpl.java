package org.ethereum.beacon.chain.pool.churn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.AttestationAggregate;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64s;

public class AttestationChurnImpl implements AttestationChurn {

  /** A queue that maintains pooled attestations. */
  private final ChurnQueue queue;
  /** A beacon chain spec. */
  private final BeaconChainSpec spec;

  private Checkpoint justifiedCheckpoint;
  private EpochNumber lowerEpoch = EpochNumber.ZERO;
  private EpochNumber upperEpoch = EpochNumber.ZERO;

  public AttestationChurnImpl(BeaconChainSpec spec, long size) {
    this.spec = spec;
    this.queue = new ChurnQueue(size);

    queue.updateEpochBoundaries(lowerEpoch, upperEpoch);
  }

  @Override
  public OffChainAggregates compute(BeaconTuple tuple) {
    assert isInitialized();

    // update epoch boundaries
    updateEpochBoundaries(
        spec.get_previous_epoch(tuple.getState()), spec.get_current_epoch(tuple.getState()));

    if (queue.isEmpty()) {
      return new OffChainAggregates(
          spec.signing_root(tuple.getBlock()), tuple.getState().getSlot(), Collections.emptyList());
    }

    // compute coverage
    Map<AttestationData, Bitlist> coverage = computeCoverage(tuple.getState());

    // check attestations against coverage and state
    MutableBeaconState state = tuple.getState().createMutableCopy();
    List<Attestation> offChainAttestations =
        queue.stream()
            .filter(
                attestation -> {
                  Bitlist bits = coverage.get(attestation.getData());
                  if (bits == null) {
                    return true;
                  }

                  return bits.and(attestation.getAggregationBits()).isEmpty();
                })
            .sorted(
                Comparator.comparing(attestation -> attestation.getData().getTarget().getEpoch()))
            .filter(
                attestation -> {
                  if (spec.verify_attestation_impl(state, attestation, false)) {
                    spec.process_attestation(state, attestation);
                    return true;
                  } else {
                    return false;
                  }
                })
            .collect(Collectors.toList());

    // compute aggregates
    List<AttestationAggregate> aggregates = computeAggregates(offChainAttestations);

    return new OffChainAggregates(
        spec.signing_root(tuple.getBlock()), tuple.getState().getSlot(), aggregates);
  }

  @Override
  public void feedFinalizedCheckpoint(Checkpoint checkpoint) {
    // finalized checkpoint takes precedence
    updateJustifiedCheckpoint(checkpoint);
  }

  @Override
  public void feedJustifiedCheckpoint(Checkpoint checkpoint) {
    // discard forks if justified checkpoint is updated
    if (justifiedCheckpoint == null
        || checkpoint.getEpoch().greater(justifiedCheckpoint.getEpoch())) {
      updateJustifiedCheckpoint(checkpoint);
    }
  }

  @Override
  public void feedNewSlot(SlotNumber slotNumber) {
    EpochNumber epoch = spec.compute_epoch_of_slot(slotNumber);
    updateEpochBoundaries(epoch.equals(EpochNumber.ZERO) ? epoch : epoch.decrement(), epoch);
  }

  private void updateJustifiedCheckpoint(Checkpoint checkpoint) {
    updateEpochBoundaries(checkpoint.getEpoch(), UInt64s.max(checkpoint.getEpoch(), upperEpoch));
    this.justifiedCheckpoint = checkpoint;
  }

  private void updateEpochBoundaries(EpochNumber newLower, EpochNumber newUpper) {
    assert newLower.lessEqual(newUpper);

    // return if there is nothing to update
    if (!(newLower.greater(lowerEpoch) || newUpper.greater(upperEpoch))) {
      return;
    }

    queue.updateEpochBoundaries(newLower, newUpper);

    lowerEpoch = newLower;
    upperEpoch = newUpper;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public void add(List<Attestation> attestations) {
    queue.add(attestations);
  }

  private Map<AttestationData, Bitlist> computeCoverage(BeaconState state) {
    Map<AttestationData, Bitlist> coverage = new HashMap<>();

    Stream.concat(
            state.getPreviousEpochAttestations().stream(),
            state.getCurrentEpochAttestations().stream())
        .forEach(
            pending -> {
              Bitlist bitlist = coverage.get(pending.getData());
              if (bitlist == null) {
                coverage.put(pending.getData(), pending.getAggregationBits());
              } else {
                coverage.put(pending.getData(), bitlist.or(pending.getAggregationBits()));
              }
            });

    return coverage;
  }

  private List<AttestationAggregate> computeAggregates(List<Attestation> attestations) {
    if (attestations.isEmpty()) {
      return Collections.emptyList();
    }

    List<AttestationAggregate> aggregates = new ArrayList<>();
    AttestationAggregate current = null;

    for (Attestation attestation : attestations) {
      if (current == null || !current.add(attestation)) {
        current = AttestationAggregate.create(attestation);
        aggregates.add(current);
      }
    }

    return aggregates;
  }
}
