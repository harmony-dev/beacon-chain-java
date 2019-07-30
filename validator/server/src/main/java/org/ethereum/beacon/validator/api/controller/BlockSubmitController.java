package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.PartiallyFailedException;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;

import java.util.Optional;

public class BlockSubmitController extends SyncRestController {
  private final MutableBeaconChain beaconChain;
  private final WireApiSub wireApiSub;
  private final BeaconChainSpec spec;

  public BlockSubmitController(
      SyncManager syncManager, WireApiSub wireApiSub, MutableBeaconChain beaconChain, BeaconChainSpec spec) {
    super(syncManager);
    this.beaconChain = beaconChain;
    this.wireApiSub = wireApiSub;
    this.spec = spec;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processPostRequestImpl(this::acceptBlockSubmit);
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
