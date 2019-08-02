package org.ethereum.beacon.validator.api;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.validator.api.controller.AttestationController;
import org.ethereum.beacon.validator.api.controller.BlockController;
import org.ethereum.beacon.validator.api.controller.ControllerRoute;
import org.ethereum.beacon.validator.api.controller.DutiesController;
import org.ethereum.beacon.validator.api.controller.ForkController;
import org.ethereum.beacon.validator.api.controller.SyncingController;
import org.ethereum.beacon.validator.api.controller.TimeController;
import org.ethereum.beacon.validator.api.controller.VersionController;
import org.ethereum.beacon.wire.PeerManager;
import org.ethereum.beacon.wire.sync.SyncManager;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

/**
 * REST service for beacon chain validator according to <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml">https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml</a>
 */
public class ValidatorRest implements ValidatorServer {
  private static final Logger logger = LogManager.getLogger(ValidatorRest.class);
  private final RestServerVerticle server;
  private String id = null;
  private Vertx vertx = Vertx.vertx();

  public ValidatorRest(
      Integer serverPort,
      BeaconChainSpec spec,
      ObservableStateProcessor stateProcessor,
      PeerManager peerManager,
      SyncManager syncManager,
      UInt64 chainId,
      ValidatorDutiesService validatorDutiesService,
      MutableBeaconChain beaconChain) {
    List<ControllerRoute> controllers = new ArrayList<>();
    controllers.add(ControllerRoute.of("/node/version", new VersionController()));
    controllers.add(ControllerRoute.of("/node/genesis_time", new TimeController(stateProcessor)));
    controllers.add(
        ControllerRoute.of("/node/syncing", new SyncingController(syncManager, peerManager)));
    controllers.add(
        ControllerRoute.of(
            "/validator/duties",
            new DutiesController(stateProcessor, validatorDutiesService, syncManager, spec)));
    controllers.add(
        ControllerRoute.of(
            "/validator/block",
            new BlockController(
                stateProcessor,
                peerManager.getWireApiSub(),
                beaconChain,
                validatorDutiesService,
                syncManager,
                spec)));
    controllers.add(
        ControllerRoute.of(
            "/validator/attestation",
            new AttestationController(
                stateProcessor,
                peerManager.getWireApiSub(),
                validatorDutiesService,
                syncManager,
                spec)));
    controllers.add(
        ControllerRoute.of("/node/fork", new ForkController(stateProcessor, spec, chainId)));
    this.server = new RestServerVerticle(serverPort, controllers);
  }

  @Override
  public synchronized void start() {
    if (id != null) {
      throw new RuntimeException(
          String.format(
              "Validator REST already started on %s port with id #%s", server.getServerPort(), id));
    }
    logger.info("Starting Validator REST on {} port", server.getServerPort());
    vertx.deployVerticle(
        server,
        event -> {
          this.id = event.result();
          logger.info("Validator REST started on {} port with id #{}", server.getServerPort(), id);
        });
  }

  @Override
  public synchronized void stop() {
    try {
      server.stop();
      vertx.undeploy(id);
      this.id = null;
    } catch (Exception e) {
      logger.error(String.format("Failed to stop Validator REST with id #%s", id), e);
    }
  }
}
