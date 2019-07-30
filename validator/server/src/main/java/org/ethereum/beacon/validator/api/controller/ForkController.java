package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.uint.UInt64;

public class ForkController extends RestController {
  private final BeaconChainSpec spec;
  private final UInt64 chainId;
  private ObservableBeaconState observableBeaconState = null;

  public ForkController(
      ObservableStateProcessor stateProcessor, BeaconChainSpec spec, UInt64 chainId) {
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.observableBeaconState = observableBeaconState;
            });
    this.spec = spec;
    this.chainId = chainId;
  }

  private Object produceForkResponse() {
    ForkResponse forkResponse =
        new ForkResponse(
            observableBeaconState.getLatestSlotState().getFork().getCurrentVersion().toString(),
            observableBeaconState.getLatestSlotState().getFork().getPreviousVersion().toString(),
            spec.slot_to_epoch(observableBeaconState.getLatestSlotState().getSlot()).longValue(),
            chainId.toBI());
    return forkResponse;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doJsonResponse(this::produceForkResponse);
  }
}
