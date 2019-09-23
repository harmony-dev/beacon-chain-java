package org.ethereum.beacon.chain.observer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.chain.pool.churn.AttestationChurn;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

public class YetAnotherStateProcessor implements ObservableStateProcessor {

  private final AttestationChurn churn;
  private final SimpleProcessor<ObservableBeaconState> stateStream;
  private final EmptySlotTransition emptySlotTransition;
  private final BeaconChainSpec spec;
  private final Scheduler scheduler;
  private final Publisher<SlotNumber> newSlots;
  private final Publisher<BeaconChainHead> chainHeads;
  private final Publisher<Checkpoint> justifiedCheckpoints;
  private final Publisher<Checkpoint> finalizedCheckpoints;
  private final Publisher<Attestation> validAttestations;

  private SlotNumber recentSlot = SlotNumber.ZERO;
  private BeaconStateEx recentState;
  private BeaconBlock recentHead;

  public YetAnotherStateProcessor(
      BeaconChainSpec spec,
      EmptySlotTransition emptySlotTransition,
      AttestationChurn churn,
      Schedulers schedulers,
      Publisher<SlotNumber> newSlots,
      Publisher<BeaconChainHead> chainHeads,
      Publisher<Checkpoint> justifiedCheckpoints,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<Attestation> validAttestations) {
    this.spec = spec;
    this.emptySlotTransition = emptySlotTransition;
    this.churn = churn;
    this.scheduler = schedulers.newSingleThreadDaemon("yet-another-state-processor").toReactor();
    this.stateStream = new SimpleProcessor<>(scheduler, "YetAnotherStateProcessor.stateStream");
    this.newSlots = newSlots;
    this.justifiedCheckpoints = justifiedCheckpoints;
    this.finalizedCheckpoints = finalizedCheckpoints;
    this.validAttestations = validAttestations;
    this.chainHeads = chainHeads;
  }

  @Override
  public void start() {
    Flux.from(chainHeads).publishOn(scheduler).subscribe(this::onNewHead);
    Flux.from(justifiedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.churn::feedJustifiedCheckpoint);
    Flux.from(finalizedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.churn::feedFinalizedCheckpoint);
    Flux.from(validAttestations)
        .publishOn(scheduler)
        .bufferTimeout(
            AttestationPool.VERIFIER_BUFFER_SIZE, AttestationPool.VERIFIER_INTERVAL, scheduler)
        .subscribe(this.churn::add);
    Flux.from(newSlots).publishOn(scheduler).subscribe(this::onNewSlot);
  }

  private void onNewHead(BeaconChainHead head) {
    recentHead = head.getBlock();
    recentState = emptySlotTransition.apply(new BeaconStateExImpl(head.getState()), recentSlot);
    publishObservableState();
  }

  private void onNewSlot(SlotNumber slot) {
    if (slot.greater(recentSlot)) {
      recentSlot = slot;
      if (recentHead != null && recentState != null) {
        recentState = emptySlotTransition.apply(recentState, slot);
        publishObservableState();
      }
    }
  }

  private void publishObservableState() {
    OffChainAggregates aggregates = churn.compute(BeaconTuple.of(recentHead, recentState));
    PendingOperations pendingOperations = new PendingOperationsImpl(aggregates);
    stateStream.onNext(new ObservableBeaconState(recentHead, recentState, pendingOperations));
  }

  @Override
  public Publisher<ObservableBeaconState> getObservableStateStream() {
    return stateStream;
  }

  private final class PendingOperationsImpl implements PendingOperations {

    private final OffChainAggregates aggregates;

    private List<Attestation> attestations;

    public PendingOperationsImpl(OffChainAggregates aggregates) {
      this.aggregates = aggregates;
    }

    @Override
    public List<Attestation> getAttestations() {
      if (attestations == null) {
        attestations =
            aggregates.getAggregates().stream()
                .map(aggregate -> aggregate.getAggregate(spec.getConstants()))
                .collect(Collectors.toList());
      }
      return attestations;
    }

    @Override
    public List<ProposerSlashing> peekProposerSlashings(int maxCount) {
      return Collections.emptyList();
    }

    @Override
    public List<AttesterSlashing> peekAttesterSlashings(int maxCount) {
      return Collections.emptyList();
    }

    @Override
    public List<Attestation> peekAggregateAttestations(int maxCount, SpecConstants specConstants) {
      if (attestations == null) {
        attestations =
            aggregates.getAggregates().stream()
                .limit(maxCount)
                .map(aggregate -> aggregate.getAggregate(specConstants))
                .collect(Collectors.toList());
      }
      return attestations.stream().limit(maxCount).collect(Collectors.toList());
    }

    @Override
    public List<VoluntaryExit> peekExits(int maxCount) {
      return Collections.emptyList();
    }

    @Override
    public List<Transfer> peekTransfers(int maxCount) {
      return Collections.emptyList();
    }
  }
}
