package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.ValidatorDutiesService;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class BlockController extends SyncRestController {
  private final ValidatorDutiesService service;
  private ObservableBeaconState observableBeaconState = null;

  public BlockController(
      ObservableStateProcessor stateProcessor,
      ValidatorDutiesService service,
      SyncManager syncManager) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream()).subscribe(this::updateState);
    this.service = service;
  }

  private synchronized void updateState(ObservableBeaconState observableBeaconState) {
    this.observableBeaconState = observableBeaconState;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processGetRequestImpl(this::produceValidatorBlockResponse);
  }

  private synchronized Object produceValidatorBlockResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(getParamString("slot", params)));
      BLSSignature randaoReveal =
          BLSSignature.wrap(Bytes96.fromHexStringStrict(getParamString("randao_reveal", params)));
      BeaconBlock block = service.prepareBlock(slot, randaoReveal, observableBeaconState);
      return BeaconBlockConverter.serialize(block);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
