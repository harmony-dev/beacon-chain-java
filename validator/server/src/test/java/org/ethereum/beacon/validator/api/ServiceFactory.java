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
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

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

  public static SyncManager createSyncManager() {
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
          SlotNumber slot,
          BLSSignature randaoReveal,
          ObservableBeaconState observableState) {
        return null;
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
