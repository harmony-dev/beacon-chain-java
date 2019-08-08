package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.core.types.Time;
import reactor.core.publisher.Flux;

public class TimeController extends RestController {
  private Time time = null;

  public TimeController(ObservableStateProcessor stateProcessor) {
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              if (time == null) {
                time = observableBeaconState.getLatestSlotState().getGenesisTime();
              }
            });
  }

  private String produceGenesisTimeResponse() {
    if (time == null) {
      throw new RuntimeException("Genesis time is not yet known!");
    }
    return "" + time.getValue();
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doStringResponse(this::produceGenesisTimeResponse);
  }
}
