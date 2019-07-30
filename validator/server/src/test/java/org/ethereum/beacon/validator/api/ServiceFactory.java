package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.validator.local.MultiValidatorServiceTest;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.Collections;
import java.util.Random;

public class ServiceFactory {

  public static ObservableStateProcessor createObservableStateProcessor(SpecConstants constants) {
    return new ObservableStateProcessor() {
      @Override
      public void start() {}

      @Override
      public Publisher<BeaconChainHead> getHeadStream() {
        return null;
      }

      @Override
      public Publisher<ObservableBeaconState> getObservableStateStream() {
        return Mono.just(
            new ObservableBeaconState(
                BeaconBlock.Builder.createEmpty()
                    .withSlot(SlotNumber.ZERO)
                    .withParentRoot(Hash32.ZERO)
                    .withStateRoot(Hash32.ZERO)
                    .withSignature(BLSSignature.ZERO)
                    .withBody(BeaconBlockBody.getEmpty(constants))
                    .build(),
                BeaconStateEx.getEmpty(),
                new PendingOperationsState(Collections.emptyList())));
      }

      @Override
      public Publisher<PendingOperations> getPendingOperationsStream() {
        return null;
      }
    };
  }

  public static ObservableStateProcessor createObservableStateProcessorGenesisTimeModifiedTo10(
      SpecConstants constants) {
    return new ObservableStateProcessor() {
      @Override
      public void start() {}

      @Override
      public Publisher<BeaconChainHead> getHeadStream() {
        return null;
      }

      @Override
      public Publisher<ObservableBeaconState> getObservableStateStream() {
        MutableBeaconState state = BeaconStateEx.getEmpty().createMutableCopy();
        state.setGenesisTime(Time.of(10));
        return Mono.just(
            new ObservableBeaconState(
                BeaconBlock.Builder.createEmpty()
                    .withSlot(SlotNumber.ZERO)
                    .withParentRoot(Hash32.ZERO)
                    .withStateRoot(Hash32.ZERO)
                    .withSignature(BLSSignature.ZERO)
                    .withBody(BeaconBlockBody.getEmpty(constants))
                    .build(),
                new BeaconStateExImpl(state.createImmutable()),
                new PendingOperationsState(Collections.emptyList())));
      }

      @Override
      public Publisher<PendingOperations> getPendingOperationsStream() {
        return null;
      }
    };
  }

  public static ObservableStateProcessor createObservableStateProcessorWithValidators(
      BLSPubkey pubkey, SpecConstants constants) {
    return new ObservableStateProcessor() {
      @Override
      public void start() {}

      @Override
      public Publisher<BeaconChainHead> getHeadStream() {
        return null;
      }

      @Override
      public Publisher<ObservableBeaconState> getObservableStateStream() {
        MutableBeaconState state = BeaconStateEx.getEmpty().createMutableCopy();
        state
            .getValidators()
            .addAll(
                MultiValidatorServiceTest.createRegistry(
                    new Random(), ValidatorIndex.of(127), pubkey, constants));
        return Mono.just(
            new ObservableBeaconState(
                BeaconBlock.Builder.createEmpty()
                    .withSlot(SlotNumber.ZERO)
                    .withParentRoot(Hash32.ZERO)
                    .withStateRoot(Hash32.ZERO)
                    .withSignature(BLSSignature.ZERO)
                    .withBody(BeaconBlockBody.getEmpty(constants))
                    .build(),
                new BeaconStateExImpl(state.createImmutable()),
                new PendingOperationsState(Collections.emptyList())));
      }

      @Override
      public Publisher<PendingOperations> getPendingOperationsStream() {
        return null;
      }
    };
  }

  public static SyncManager createSyncManagerSyncNotStarted() {
    return new SyncManager() {
      @Override
      public Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport() {
        return null;
      }

      @Override
      public void start() {}

      @Override
      public void stop() {}

      @Override
      public Publisher<SyncMode> getSyncModeStream() {
        return null;
      }

      @Override
      public Publisher<SyncStatus> getSyncStatusStream() {
        return Mono.just(new SyncStatus(false, null, null, null, null));
      }

      @Override
      public Disposable subscribeToOnlineBlocks(Publisher<Feedback<BeaconBlock>> onlineBlocks) {
        return null;
      }

      @Override
      public Disposable subscribeToFinalizedBlocks(Publisher<BeaconBlock> finalBlocks) {
        return null;
      }

      @Override
      public void setSyncApi(WireApiSync syncApi) {}
    };
  }

  public static SyncManager createSyncManagerSyncStarted() {
    return new SyncManager() {
      @Override
      public Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport() {
        return null;
      }

      @Override
      public void start() {}

      @Override
      public void stop() {}

      @Override
      public Publisher<SyncMode> getSyncModeStream() {
        return null;
      }

      @Override
      public Publisher<SyncStatus> getSyncStatusStream() {
        return Mono.just(
            new SyncStatus(
                true, SlotNumber.ZERO, SlotNumber.of(1000), SlotNumber.ZERO, SyncMode.Long));
      }

      @Override
      public Disposable subscribeToOnlineBlocks(Publisher<Feedback<BeaconBlock>> onlineBlocks) {
        return null;
      }

      @Override
      public Disposable subscribeToFinalizedBlocks(Publisher<BeaconBlock> finalBlocks) {
        return null;
      }

      @Override
      public void setSyncApi(WireApiSync syncApi) {}
    };
  }

  public static SyncManager createSyncManagerShortSync() {
    return new SyncManager() {
      @Override
      public Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport() {
        return null;
      }

      @Override
      public void start() {}

      @Override
      public void stop() {}

      @Override
      public Publisher<SyncMode> getSyncModeStream() {
        return null;
      }

      @Override
      public Publisher<SyncStatus> getSyncStatusStream() {
        return Mono.just(
            new SyncStatus(
                true, SlotNumber.ZERO, SlotNumber.ZERO, SlotNumber.ZERO, SyncMode.Short));
      }

      @Override
      public Disposable subscribeToOnlineBlocks(Publisher<Feedback<BeaconBlock>> onlineBlocks) {
        return null;
      }

      @Override
      public Disposable subscribeToFinalizedBlocks(Publisher<BeaconBlock> finalBlocks) {
        return null;
      }

      @Override
      public void setSyncApi(WireApiSync syncApi) {}
    };
  }

  public static ValidatorDutiesService createValidatorDutiesService(SpecConstants constants) {
    return new ValidatorDutiesService(BeaconChainSpec.createWithDefaults(), null, null) {
      @Override
      public BeaconBlock prepareBlock(
          SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableBeaconState) {
        BeaconBlock.Builder builder =
            BeaconBlock.Builder.createEmpty()
                .withSlot(slot)
                .withParentRoot(Hash32.ZERO)
                .withStateRoot(Hash32.ZERO)
                .withSignature(BLSSignature.ZERO)
                .withBody(BeaconBlockBody.getEmpty(constants));

        return builder.build();
      }

      @Override
      public Attestation prepareAttestation(
          SlotNumber slot,
          ValidatorIndex validatorIndex,
          ShardNumber shard,
          ObservableBeaconState observableBeaconState) {
        return new Attestation(
            Bitlist.of(0),
            new AttestationData(
                Hash32.ZERO,
                Checkpoint.EMPTY,
                Checkpoint.EMPTY,
                new Crosslink(shard, Hash32.ZERO, EpochNumber.ZERO, EpochNumber.ZERO, Hash32.ZERO)),
            Bitlist.of(0),
            BLSSignature.ZERO,
            constants);
      }
    };
  }

  public static WireApiSub createWireApiSub() {
    return new WireApiSub() {
      @Override
      public void sendProposedBlock(BeaconBlock block) {}

      @Override
      public void sendAttestation(Attestation attestation) {}

      @Override
      public Publisher<BeaconBlock> inboundBlocksStream() {
        return null;
      }

      @Override
      public Publisher<Attestation> inboundAttestationsStream() {
        return null;
      }
    };
  }

  public static WireApiSub createWireApiSubWithMirror() {
    return new WireApiSub() {
      SimpleProcessor<BeaconBlock> blockProcessor =
          new SimpleProcessor<BeaconBlock>(
              Schedulers.createDefault().newSingleThreadDaemon("blockProcessor"), "blockProcessor");
      SimpleProcessor<Attestation> attestationProcessor =
          new SimpleProcessor<Attestation>(
              Schedulers.createDefault().newSingleThreadDaemon("attestationProcessor"),
              "attestationProcessor");

      @Override
      public void sendProposedBlock(BeaconBlock block) {
        blockProcessor.onNext(block);
      }

      @Override
      public void sendAttestation(Attestation attestation) {
        attestationProcessor.onNext(attestation);
      }

      @Override
      public Publisher<BeaconBlock> inboundBlocksStream() {
        return blockProcessor;
      }

      @Override
      public Publisher<Attestation> inboundAttestationsStream() {
        return attestationProcessor;
      }
    };
  }

  public static MutableBeaconChain createMutableBeaconChain() {
    return new MutableBeaconChain() {
      @Override
      public ImportResult insert(BeaconBlock block) {
        return ImportResult.NoParent;
      }

      @Override
      public Publisher<BeaconTupleDetails> getBlockStatesStream() {
        return null;
      }

      @Override
      public BeaconTuple getRecentlyProcessed() {
        return null;
      }

      @Override
      public void init() {}
    };
  }
}
