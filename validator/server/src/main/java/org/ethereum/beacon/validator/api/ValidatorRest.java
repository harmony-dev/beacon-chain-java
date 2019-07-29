package org.ethereum.beacon.validator.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * REST service for beacon chain validator according to <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml">https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml</a>
 */
public class ValidatorRest extends AbstractVerticle {

  private final Integer serverPort;

  private final BeaconChainSpec spec;
  private final WireApiSub wireApiSub;
  private final MutableBeaconChain beaconChain;
  private final ValidatorDutiesService validatorDutiesService;
  private final UInt64 chainId;

  private String version = null;
  private Time time = null;
  private SyncingResponse syncingResponse = null;
  private boolean shortSync = false;
  private ObservableBeaconState observableBeaconState = null;

  public ValidatorRest(
      Integer serverPort,
      BeaconChainSpec spec,
      ObservableStateProcessor stateProcessor,
      SyncManager syncManager,
      UInt64 chainId,
      ValidatorDutiesService validatorDutiesService,
      WireApiSub wireApiSub,
      MutableBeaconChain beaconChain) {
    this.serverPort = serverPort;
    this.spec = spec;
    this.wireApiSub = wireApiSub;
    this.beaconChain = beaconChain;
    this.chainId = chainId;
    this.validatorDutiesService = validatorDutiesService;

    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.observableBeaconState = observableBeaconState;
              if (time == null) {
                time = observableBeaconState.getLatestSlotState().getGenesisTime();
              }
            });

    Flux.from(syncManager.getSyncStatusStream())
        .subscribe(
            syncStatus -> {
              shortSync = SyncManager.SyncMode.Short.equals(syncStatus.getSyncMode());
              syncingResponse =
                  new SyncingResponse(
                      syncStatus.isSyncing(),
                      syncStatus.getStart() == null
                          ? BigInteger.ZERO
                          : syncStatus.getStart().toBI(),
                      syncStatus.getCurrent() == null
                          ? BigInteger.ZERO
                          : syncStatus.getCurrent().toBI(),
                      syncStatus.getBestKnown() == null
                          ? BigInteger.ZERO
                          : syncStatus.getBestKnown().toBI());
            });
  }

  static String getParamString(String param, MultiMap params) throws InvalidInputException {
    try {
      assert params.contains(param);
      assert params.getAll(param).size() == 1;
      return params.get(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain required param `%s`", param));
    }
  }

  static List<String> getParamStringList(String param, MultiMap params)
      throws InvalidInputException {
    try {
      assert params.contains(param);
      return params.getAll(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain required param `%s`", param));
    }
  }

  @Override
  public void start() {
    // Create a router object.
    Router router = Router.router(vertx);
    router.get("/node/version").handler(doJsonResponse(this::produceVersionResponse));
    router.get("/node/genesis_time").handler(doJsonResponse(this::produceGenesisTimeResponse));
    router.get("/node/syncing").handler(doJsonResponse(this::produceSyncingResponse));
    router
        .get("/validator/duties")
        .handler(processGetRequestImpl(this::produceValidatorDutiesResponse));
    router
        .get("/validator/block")
        .handler(processGetRequestImpl(this::produceValidatorBlockResponse));
    router.post("/validator/block").handler(processPostRequestImpl(this::acceptBlockSubmit));
    router
        .get("/validator/attestation")
        .handler(processGetRequestImpl(this::produceAttestationResponse));
    router
        .post("/validator/attestation")
        .handler(processPostRequestImpl(this::acceptAttestationSubmit));
    router.get("/node/fork").handler(doJsonResponse(this::produceForkResponse));
    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx.createHttpServer().requestHandler(router).listen(serverPort);
  }

  /**
   * Produces json response using supplier
   *
   * @param response Json string supplier
   * @return request/response handling function
   */
  private Handler<RoutingContext> doJsonResponse(Supplier<Object> response) {
    return event ->
        event
            .response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(response.get()));
  }

  private String produceVersionResponse() {
    if (version == null) {
      Properties props = loadResources("rest-server.properties", this.getClass().getClassLoader());
      final String version = props.getProperty("versionNumber");
      this.version = String.format("Beacon Chain Java %s", version);
    }
    return version;
  }

  private Properties loadResources(final String name, final ClassLoader classLoader) {
    try {
      final Enumeration<URL> systemResources =
          (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
              .getResources(name);
      Properties props = new Properties();
      props.load(systemResources.nextElement().openStream());

      return props;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Object produceSyncingResponse() {
    return syncingResponse;
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

  private Object produceValidatorDutiesResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      EpochNumber epoch;
      BeaconStateEx stateEx = observableBeaconState.getLatestSlotState();
      if (params.contains("epoch")) {
        epoch = EpochNumber.castFrom(UInt64.valueOf(getParamString("epoch", params)));
      } else { // Epoch is not required
        epoch = spec.get_current_epoch(stateEx).increment();
      }

      List<BLSPubkey> pubKeys =
          getParamStringList("validator_pubkeys", params).stream()
              .map(Bytes48::fromHexStringStrict)
              .map(BLSPubkey::wrap)
              .collect(Collectors.toList());
      if (!epoch.lessEqual(spec.get_current_epoch(stateEx).increment())) {
        throw new NotAcceptableInputException("Couldn't provide duties for requested epoch");
      }
      Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> validatorDuties =
          validatorDutiesService.getValidatorDuties(stateEx, epoch);

      List<ValidatorDutiesResponse.ValidatorDuty> responseList = new ArrayList<>();
      for (BLSPubkey pubkey : pubKeys) {
        ValidatorIndex validatorIndex = spec.get_validator_index_by_pubkey(stateEx, pubkey);
        Triplet<BigInteger, Integer, BigInteger> duty =
            validatorDutiesService.findDutyForValidator(validatorIndex, validatorDuties);
        if (duty.getValue1() != null) {
          ValidatorDutiesResponse.ValidatorDuty dutyResponse =
              new ValidatorDutiesResponse.ValidatorDuty();
          dutyResponse.setValidatorPubkey(pubkey.toHexString());
          dutyResponse.setBlockProposalSlot(duty.getValue0());
          dutyResponse.setAttestationSlot(duty.getValue2());
          dutyResponse.setAttestationShard(duty.getValue1());
          responseList.add(dutyResponse);
        }
      }

      return new ValidatorDutiesResponse(responseList);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (InvalidInputException | NotAcceptableInputException ex) {
      throw ex;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Object produceValidatorBlockResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(getParamString("slot", params)));
      BLSSignature randaoReveal =
          BLSSignature.wrap(Bytes96.fromHexStringStrict(getParamString("randao_reveal", params)));
      BeaconBlock block =
          validatorDutiesService.prepareBlock(slot, randaoReveal, observableBeaconState);
      return BeaconBlockConverter.serialize(block);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Throwable> acceptBlockSubmit(String body) {
    final BlockSubmit submitData = Json.decodeValue(body, BlockSubmit.class);

    try {
      BeaconBlock block = submitData.createBeaconBlock();
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

  private Object produceAttestationResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(getParamString("slot", params)));
      BLSPubkey validatorPubkey =
          BLSPubkey.wrap(Bytes48.fromHexStringStrict(getParamString("validator_pubkey", params)));
      Long pocBit =
          Long.valueOf(
              getParamString("poc_bit", params)); // XXX: Proof of custody is a stub at Phase 0
      ShardNumber shard = ShardNumber.of(UInt64.valueOf(getParamString("shard", params)));

      ValidatorIndex validatorIndex =
          spec.get_validator_index_by_pubkey(
              observableBeaconState.getLatestSlotState().createMutableCopy(), validatorPubkey);
      Attestation attestation =
          validatorDutiesService.prepareAttestation(
              slot, validatorIndex, shard, observableBeaconState);
      IndexedAttestation indexedAttestation =
          spec.convert_to_indexed(
              observableBeaconState.getLatestSlotState().createMutableCopy(), attestation);
      return BeaconBlockConverter.presentIndexedAttestation(indexedAttestation);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Throwable> acceptAttestationSubmit(String body) {
    final AttestationSubmit submitData = Json.decodeValue(body, AttestationSubmit.class);
    try {
      IndexedAttestation indexedAttestation = submitData.createAttestation();
      // Verification
      MutableBeaconState state = observableBeaconState.getLatestSlotState().createMutableCopy();
      List<ValidatorIndex> committee =
          spec.get_crosslink_committee(
              state,
              indexedAttestation.getData().getTargetEpoch(),
              indexedAttestation.getData().getCrosslink().getShard());
      Bitfield bitfield =
          new Bitfield(
              committee.size(),
              indexedAttestation.getCustodyBit0Indices().listCopy().stream()
                  .map(ValidatorIndex::intValue)
                  .collect(Collectors.toList()));
      Attestation attestation =
          new Attestation(
              bitfield, indexedAttestation.getData(), bitfield, indexedAttestation.getSignature());
      try {
        if (new AttestationVerifier(spec).verify(attestation, state).isPassed()) {
          spec.process_attestation(state, attestation);
        } else {
          throw new PartiallyFailedException("Verification not passed for attestation");
        }
      } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
        throw new PartiallyFailedException(ex);
      } finally {
        // Broadcast
        wireApiSub.sendAttestation(attestation);
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

  private String produceGenesisTimeResponse() {
    if (time == null) {
      throw new RuntimeException("Genesis time is not yet known!");
    }
    return "" + time.getValue();
  }

  /**
   * GET Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   *
   * <p>Responses with 406 NOT ACCEPTABLE when catches {@link NotAcceptableInputException} from func
   * function
   *
   * <p>Responses with 500 INTERNAL SERVER ERROR for all other errors
   */
  private Handler<RoutingContext> processGetRequestImpl(
      Function<HttpServerRequest, Object> response) {
    return event -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        event.response().setStatusCode(503).end();
      } else {
        try {
          event
              .response()
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(response.apply(event.request())));
        } catch (InvalidInputException ex) {
          event.response().setStatusCode(400).end();
        } catch (NotAcceptableInputException ex) {
          event.response().setStatusCode(406).end();
        } catch (Exception ex) {
          event.response().setStatusCode(500).end();
        }
      }
    };
  }

  /**
   * POST Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 202 ACCEPTED when catches {@link PartiallyFailedException} from func function
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   *
   * <p>Responses with 406 NOT ACCEPTABLE when catches {@link NotAcceptableInputException} from func
   * function
   *
   * <p>Responses with 500 INTERNAL SERVER ERROR for all other errors
   */
  private Handler<RoutingContext> processPostRequestImpl(
      Function<String, Optional<Throwable>> func) {
    return event -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        event.response().setStatusCode(503).end();
      } else {
        Optional<Throwable> result = func.apply(event.getBodyAsString());
        if (!result.isPresent()) {
          event.response().setStatusCode(200).end();
        } else {
          Throwable ex = result.get();
          if (ex instanceof PartiallyFailedException) {
            event.response().setStatusCode(202).end();
          } else if (ex instanceof InvalidInputException) {
            event.response().setStatusCode(400).end();
          } else if (ex instanceof NotAcceptableInputException) {
            event.response().setStatusCode(406).end();
          } else {
            event.response().setStatusCode(500).end();
          }
        }
      }
      ;
    };
  }
}
