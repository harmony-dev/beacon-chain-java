package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.MultiValidatorServiceTest;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.Random;

public class ServiceFactory {

  public static ObservableStateProcessor createObservableStateProcessor() {
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
                    .withBody(BeaconBlockBody.EMPTY)
                    .build(),
                BeaconStateEx.getEmpty(),
                PendingOperations.getEmpty()));
      }

      @Override
      public Publisher<PendingOperations> getPendingOperationsStream() {
        return null;
      }
    };
  }

  public static ObservableStateProcessor createObservableStateProcessorGenesisTimeModifiedTo10() {
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
                    .withBody(BeaconBlockBody.EMPTY)
                    .build(),
                new BeaconStateExImpl(state.createImmutable()),
                PendingOperations.getEmpty()));
      }

      @Override
      public Publisher<PendingOperations> getPendingOperationsStream() {
        return null;
      }
    };
  }

  public static ObservableStateProcessor createObservableStateProcessorWithValidators(
      BLSPubkey pubkey) {
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
            .getValidatorRegistry()
            .addAll(
                MultiValidatorServiceTest.createRegistry(
                    new Random(), ValidatorIndex.of(127), pubkey));
        return Mono.just(
            new ObservableBeaconState(
                BeaconBlock.Builder.createEmpty()
                    .withSlot(SlotNumber.ZERO)
                    .withParentRoot(Hash32.ZERO)
                    .withStateRoot(Hash32.ZERO)
                    .withSignature(BLSSignature.ZERO)
                    .withBody(BeaconBlockBody.EMPTY)
                    .build(),
                new BeaconStateExImpl(state.createImmutable()),
                PendingOperations.getEmpty()));
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
      public void setSyncApi(WireApiSync syncApi) {

      }
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
      public void setSyncApi(WireApiSync syncApi) {

      }
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
      public void setSyncApi(WireApiSync syncApi) {

      }
    };
  }

  public static BeaconChainProposer createBeaconChainProposer() {
    return new BeaconChainProposer() {
      @Override
      public BeaconBlock propose(
          ObservableBeaconState observableState, MessageSigner<BLSSignature> signer) {
        return null;
      }

      @Override
      public BeaconBlock.Builder prepareBuilder(
          SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableState) {
        return null;
      }
    };
  }

  public static BeaconChainProposer createBeaconChainProposerWithPredefinedBuilder() {
    return new BeaconChainProposer() {
      @Override
      public BeaconBlock propose(
          ObservableBeaconState observableState, MessageSigner<BLSSignature> signer) {
        return null;
      }

      @Override
      public BeaconBlock.Builder prepareBuilder(
          SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableState) {
        BeaconBlock.Builder builder =
            BeaconBlock.Builder.createEmpty()
                .withSlot(slot)
                .withParentRoot(Hash32.ZERO)
                .withStateRoot(Hash32.ZERO)
                .withSignature(BLSSignature.ZERO)
                .withBody(BeaconBlockBody.EMPTY);

        return builder;
      }
    };
  }

  public static BeaconChainAttester createBeaconChainAttester() {
    return new BeaconChainAttester() {
      @Override
      public Attestation attest(
          ValidatorIndex validatorIndex,
          ShardNumber shard,
          ObservableBeaconState observableState,
          MessageSigner<BLSSignature> signer) {
        return null;
      }

      @Override
      public Attestation prepareAttestation(
          ValidatorIndex validatorIndex,
          ShardNumber shard,
          ObservableBeaconState observableState,
          SlotNumber slot) {
        return null;
      }
    };
  }

  public static BeaconChainAttester createBeaconChainAttesterWithPredefinedPrepare() {
    return new BeaconChainAttester() {
      @Override
      public Attestation attest(
          ValidatorIndex validatorIndex,
          ShardNumber shard,
          ObservableBeaconState observableState,
          MessageSigner<BLSSignature> signer) {
        return null;
      }

      @Override
      public Attestation prepareAttestation(
          ValidatorIndex validatorIndex,
          ShardNumber shard,
          ObservableBeaconState observableState,
          SlotNumber slot) {
        return new Attestation(
            Bitfield.EMPTY,
            new AttestationData(
                Hash32.ZERO,
                EpochNumber.ZERO,
                Hash32.ZERO,
                EpochNumber.ZERO,
                Hash32.ZERO,
                new Crosslink(
                    shard,
                    EpochNumber.ZERO,
                    EpochNumber.ZERO,
                    Hash32.ZERO,
                    Hash32.ZERO)),
            Bitfield.EMPTY,
            BLSSignature.ZERO);
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
