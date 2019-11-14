package org.ethereum.beacon.validator.api;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
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
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.validator.api.controller.ControllerRoute;
import org.ethereum.beacon.validator.api.controller.VersionController;
import org.ethereum.beacon.validator.local.MultiValidatorServiceTest;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.Peer;
import org.ethereum.beacon.wire.PeerManager;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ServiceFactory {

  /** {@link VersionController} uses data which is accessible only in assembled jar */
  public static ValidatorServer createVersionOverride(Integer serverPort) {
    return new ValidatorServer() {
      private RestServerVerticle server =
          new RestServerVerticle(
              serverPort,
              new ArrayList<ControllerRoute>() {
                {
                  add(
                      ControllerRoute.of(
                          "/node/version",
                          new VersionController() {
                            @Override
                            public Handler<RoutingContext> getHandler() {
                              return event ->
                                  event
                                      .response()
                                      .putHeader("content-type", "application/json; charset=utf-8")
                                      .end("Beacon Chain Java validator-server v0.2.0");
                            }
                          }));
                }
              });
      private String id;
      private Vertx vertx = Vertx.vertx();

      @Override
      public void start() {
        vertx.deployVerticle(
            server,
            event -> {
              this.id = event.result();
            });
      }

      @Override
      public void stop() {
        try {
          server.stop();
          vertx.undeploy(id);
          this.id = null;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

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
        return Mono.just(SyncMode.Long);
      }

      @Override
      public Publisher<Boolean> getIsSyncingStream() {
        return Mono.just(false);
      }

      @Override
      public Publisher<SlotNumber> getStartSlotStream() {
        return Mono.just(SlotNumber.ZERO);
      }

      @Override
      public Publisher<SlotNumber> getLastSlotStream() {
        return Mono.just(SlotNumber.ZERO);
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
        return Mono.just(SyncMode.Long);
      }

      @Override
      public Publisher<Boolean> getIsSyncingStream() {
        return Mono.just(true);
      }

      @Override
      public Publisher<SlotNumber> getStartSlotStream() {
        return Mono.just(SlotNumber.ZERO);
      }

      @Override
      public Publisher<SlotNumber> getLastSlotStream() {
        return Mono.just(SlotNumber.ZERO);
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
        return Mono.just(SyncMode.Short);
      }

      @Override
      public Publisher<Boolean> getIsSyncingStream() {
        return Mono.just(true);
      }

      @Override
      public Publisher<SlotNumber> getStartSlotStream() {
        return Mono.just(SlotNumber.ZERO);
      }

      @Override
      public Publisher<SlotNumber> getLastSlotStream() {
        return Mono.just(SlotNumber.ZERO);
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
          CommitteeIndex committeeIndex,
          ObservableBeaconState observableBeaconState) {
        return new Attestation(
            Bitlist.of(0),
            new AttestationData(
                SlotNumber.of(0),
                CommitteeIndex.of(0),
                Hash32.ZERO,
                Checkpoint.EMPTY,
                Checkpoint.EMPTY),
            Bitlist.of(0),
            BLSSignature.ZERO,
            constants);
      }
    };
  }

  public static PeerManager createPeerManagerWithSubMirror() {
    final WireApiSub wireApiSub =
        new WireApiSub() {
          SimpleProcessor<BeaconBlock> blockProcessor =
              new SimpleProcessor<BeaconBlock>(
                  Schedulers.createDefault().newSingleThreadDaemon("blockProcessor"),
                  "blockProcessor");
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

    return new PeerManager() {
      @Override
      public Publisher<Peer> connectedPeerStream() {
        return null;
      }

      @Override
      public Publisher<Peer> disconnectedPeerStream() {
        return null;
      }

      @Override
      public Publisher<Peer> activatedPeerStream() {
        return null;
      }

      @Override
      public WireApiSync getWireApiSync() {
        return null;
      }

      @Override
      public WireApiSub getWireApiSub() {
        return wireApiSub;
      }

      @Override
      public Publisher<SlotNumber> getMaxSlotStream() {
        return Mono.just(SlotNumber.of(1000));
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
