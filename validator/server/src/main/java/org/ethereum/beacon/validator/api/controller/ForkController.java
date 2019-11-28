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
    Flux.from(stateProcessor.getObservableStateStream()).subscribe(this::updateState);
    this.spec = spec;
    this.chainId = chainId;
  }

  private void updateState(ObservableBeaconState observableBeaconState) {
    this.observableBeaconState = observableBeaconState;
  }

  private Object produceForkResponse() {
    final ObservableBeaconState observableBeaconStateCopy = observableBeaconState;
    ForkResponse forkResponse =
        new ForkResponse(
            observableBeaconStateCopy.getLatestSlotState().getFork().getCurrentVersion().toString(),
            observableBeaconStateCopy
                .getLatestSlotState()
                .getFork()
                .getPreviousVersion()
                .toString(),
            spec.compute_epoch_at_slot(observableBeaconStateCopy.getLatestSlotState().getSlot())
                .longValue(),
            chainId.toBI());
    return forkResponse;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doJsonResponse(this::produceForkResponse);
  }
}
