package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.PartiallyFailedException;
import org.ethereum.beacon.validator.api.ValidatorDutiesService;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Optional;

public class BlockController extends SyncRestController {
  private final MutableBeaconChain beaconChain;
  private final WireApiSub wireApiSub;
  private final ValidatorDutiesService service;
  private final BeaconChainSpec spec;
  private ObservableBeaconState observableBeaconState = null;

  public BlockController(
      ObservableStateProcessor stateProcessor,
      WireApiSub wireApiSub,
      MutableBeaconChain beaconChain,
      ValidatorDutiesService service,
      SyncManager syncManager,
      BeaconChainSpec spec) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream()).subscribe(this::updateState);
    this.wireApiSub = wireApiSub;
    this.beaconChain = beaconChain;
    this.spec = spec;
    this.service = service;
  }

  private void updateState(ObservableBeaconState observableBeaconState) {
    this.observableBeaconState = observableBeaconState;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processGetRequestImpl(this::produceValidatorBlockResponse);
  }

  @Override
  public Handler<RoutingContext> postHandler() {
    return processPostRequestImpl(this::acceptBlockSubmit);
  }

  private Object produceValidatorBlockResponse(HttpServerRequest request) {
    try {
      final ObservableBeaconState observableBeaconStateCopy = observableBeaconState;
      MultiMap params = request.params();
      SlotNumber slot =
          SlotNumber.castFrom(UInt64.valueOf(ControllerUtils.getParamString("slot", params)));
      BLSSignature randaoReveal =
          BLSSignature.wrap(
              Bytes96.fromHexStringStrict(ControllerUtils.getParamString("randao_reveal", params)));
      BeaconBlock block = service.prepareBlock(slot, randaoReveal, observableBeaconStateCopy);
      return BeaconBlockConverter.serialize(block);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Throwable> acceptBlockSubmit(String body) {
    try {
      final BlockSubmit submitData = Json.decodeValue(body, BlockSubmit.class);
      BeaconBlock block = submitData.createBeaconBlock(spec.getConstants());
      // Import
      MutableBeaconChain.ImportResult importResult = beaconChain.insert(block);
      // Broadcast
      wireApiSub.sendProposedBlock(block);
      if (!MutableBeaconChain.ImportResult.OK.equals(importResult)) {
        throw new PartiallyFailedException(importResult.toString());
      }
      return Optional.empty();
    } catch (PartiallyFailedException e) {
      return Optional.of(e);
    } catch (AssertionError ex) {
      return Optional.of(new InvalidInputException(ex));
    } catch (Exception e) {
      return Optional.of(new RuntimeException(e));
    }
  }
}
