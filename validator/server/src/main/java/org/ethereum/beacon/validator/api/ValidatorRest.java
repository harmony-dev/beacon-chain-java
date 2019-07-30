package org.ethereum.beacon.validator.api;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.validator.api.controller.AttestationController;
import org.ethereum.beacon.validator.api.controller.AttestationSubmitController;
import org.ethereum.beacon.validator.api.controller.BlockController;
import org.ethereum.beacon.validator.api.controller.BlockSubmitController;
import org.ethereum.beacon.validator.api.controller.ControllerRoute;
import org.ethereum.beacon.validator.api.controller.DutiesController;
import org.ethereum.beacon.validator.api.controller.ForkController;
import org.ethereum.beacon.validator.api.controller.SyncingController;
import org.ethereum.beacon.validator.api.controller.TimeController;
import org.ethereum.beacon.validator.api.controller.VersionController;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.beacon.validator.api.controller.ControllerRoute.RequestType.GET;
import static org.ethereum.beacon.validator.api.controller.ControllerRoute.RequestType.POST;

/**
 * REST service for beacon chain validator according to <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml">https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml</a>
 */
public class ValidatorRest {
  private static final Logger logger = LogManager.getLogger(ValidatorRest.class);
  private final RestServerVerticle server;
  private String id;
  private Vertx vertx = Vertx.vertx();

  public ValidatorRest(
      Integer serverPort,
      BeaconChainSpec spec,
      ObservableStateProcessor stateProcessor,
      SyncManager syncManager,
      UInt64 chainId,
      ValidatorDutiesService validatorDutiesService,
      WireApiSub wireApiSub,
      MutableBeaconChain beaconChain) {
    List<ControllerRoute> controllers = new ArrayList<>();
    controllers.add(ControllerRoute.of(GET, "/node/version", new VersionController()));
    controllers.add(
        ControllerRoute.of(GET, "/node/genesis_time", new TimeController(stateProcessor)));
    controllers.add(ControllerRoute.of(GET, "/node/syncing", new SyncingController(syncManager)));
    controllers.add(
        ControllerRoute.of(
            GET,
            "/validator/duties",
            new DutiesController(stateProcessor, validatorDutiesService, syncManager, spec)));
    controllers.add(
        ControllerRoute.of(
            GET,
            "/validator/block",
            new BlockController(stateProcessor, validatorDutiesService, syncManager)));
    controllers.add(
        ControllerRoute.of(
            POST,
            "/validator/block",
            new BlockSubmitController(syncManager, wireApiSub, beaconChain, spec)));
    controllers.add(
        ControllerRoute.of(
            GET,
            "/validator/attestation",
            new AttestationController(stateProcessor, validatorDutiesService, syncManager, spec)));
    controllers.add(
        ControllerRoute.of(
            POST,
            "/validator/attestation",
            new AttestationSubmitController(syncManager, wireApiSub, stateProcessor, spec)));
    controllers.add(
        ControllerRoute.of(GET, "/node/fork", new ForkController(stateProcessor, spec, chainId)));
    server = new RestServerVerticle(serverPort, controllers);
  }

  public void start() {
    logger.info("Starting Validator REST on {} port", server.getServerPort());
    vertx.deployVerticle(
        server,
        event -> {
          id = event.result();
          logger.info("Validator REST started on {} port with id #{}", server.getServerPort(), id);
        });
  }

  public void stop() {
    try {
      server.stop();
      vertx.undeploy(id);
      this.id = null;
    } catch (Exception e) {
      logger.error(String.format("Failed to stop Validator REST with id #%s", id), e);
    }
  }
}
