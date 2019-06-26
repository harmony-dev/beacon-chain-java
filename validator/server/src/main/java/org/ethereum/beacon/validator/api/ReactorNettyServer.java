package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.convert.BlockDataToBlock;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.resources.LoopResources;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import javax.ws.rs.NotAcceptableException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReactorNettyServer implements RestServer {

  private final BeaconChainSpec spec;
  private final WireApiSub wireApiSub;
  private final MutableBeaconChain beaconChain;
  private final ValidatorDutiesService validatorDutiesService;
  private final UInt64 chainId;

  private DisposableServer server;
  private LoopResources serverRes;
  private ObjectMapper mapper = new ObjectMapper();
  private VersionResponse versionResponse = null;
  private TimeResponse timeResponse = null;
  private SyncingResponse syncingResponse = null;
  private boolean shortSync = false;
  private ObservableBeaconState observableBeaconState = null;

  public ReactorNettyServer(
      String serverHost,
      Integer serverPort,
      BeaconChainSpec spec,
      ObservableStateProcessor stateProcessor,
      SyncManager syncManager,
      UInt64 chainId,
      ValidatorDutiesService validatorDutiesService,
      WireApiSub wireApiSub,
      MutableBeaconChain beaconChain) {
    this.spec = spec;
    this.wireApiSub = wireApiSub;
    this.beaconChain = beaconChain;
    this.chainId = chainId;
    this.validatorDutiesService = validatorDutiesService;

    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.observableBeaconState = observableBeaconState;
              if (timeResponse == null) {
                timeResponse =
                    new TimeResponse(
                        observableBeaconState.getLatestSlotState().getGenesisTime().getValue());
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

    serverRes = LoopResources.create("server");
    HttpServer httpServer =
        HttpServer.create()
            .tcpConfiguration(tcpServer -> tcpServer.runOn(serverRes))
            .host(serverHost)
            .port(serverPort);
    final HttpServer serverWithRoutes = addRoutes(httpServer);
    this.server = serverWithRoutes.bindNow();
  }

  /** Adopted from https://stackoverflow.com/a/13592567 */
  public static Map<String, List<String>> splitQuery(URI uri) throws UnsupportedEncodingException {
    final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
    final String[] pairs = uri.getQuery().split("&");
    for (String pair : pairs) {
      final int idx = pair.indexOf("=");
      final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
      if (!query_pairs.containsKey(key)) {
        query_pairs.put(key, new LinkedList<String>());
      }
      final String value =
          idx > 0 && pair.length() > idx + 1
              ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
              : null;
      query_pairs.get(key).add(value);
    }
    return query_pairs;
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

  private HttpServer addRoutes(HttpServer httpServer) {
    return httpServer.route(
        routes ->
            routes
                .get("/node/version", wrapJsonSupplier(this::produceVersionResponse))
                .get("/node/genesis_time", wrapJsonSupplier(this::produceGenesisTimeResponse))
                .get("/node/syncing", wrapJsonSupplier(this::produceSyncingResponse))
                .get("/validator/duties", wrapJsonFunction(this::produceValidatorDutiesResponse))
                .get("/validator/block", wrapJsonFunction(this::produceValidatorBlockResponse))
                .post("/validator/block", acceptPostData(this::acceptBlockSubmit))
                .get("/validator/attestation", wrapJsonFunction(this::produceAttestationResponse))
                .post("/validator/attestation", acceptPostData(this::acceptAttestationSubmit))
                .get("/node/fork", wrapJsonSupplier(this::produceForkResponse)));
  }

  private String produceVersionResponse() {
    if (versionResponse == null) {
      Properties props = loadResources("rest-server.properties", this.getClass().getClassLoader());
      final String version = props.getProperty("versionNumber");
      this.versionResponse = new VersionResponse(String.format("Beacon Chain Java %s", version));
    }
    try {
      return mapper.writeValueAsString(versionResponse);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String produceSyncingResponse() {
    try {
      return mapper.writeValueAsString(syncingResponse);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String produceForkResponse() {
    try {
      ForkResponse forkResponse =
          new ForkResponse(
              observableBeaconState.getLatestSlotState().getFork().getCurrentVersion().toString(),
              observableBeaconState.getLatestSlotState().getFork().getPreviousVersion().toString(),
              spec.slot_to_epoch(observableBeaconState.getLatestSlotState().getSlot()).longValue(),
              chainId.toBI());
      return mapper.writeValueAsString(forkResponse);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String produceValidatorDutiesResponse(HttpServerRequest request) {
    try {
      Map<String, List<String>> params = splitQuery(new URI(request.uri()));
      assert params.containsKey("validator_pubkeys");

      EpochNumber epoch;
      BeaconStateEx stateEx = observableBeaconState.getLatestSlotState();
      if (params.containsKey("epoch")) {
        epoch = EpochNumber.castFrom(UInt64.valueOf(params.get("epoch").get(0)));
      } else { // Epoch is not required
        epoch = spec.get_current_epoch(stateEx).increment();
      }

      List<BLSPubkey> pubKeys =
          params.get("validator_pubkeys").stream()
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

      return mapper.writeValueAsString(new ValidatorDutiesResponse(responseList));
    } catch (AssertionError
        | UnsupportedEncodingException
        | URISyntaxException
        | IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (NotAcceptableInputException ex) {
      throw ex;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String produceValidatorBlockResponse(HttpServerRequest request) {
    try {
      Map<String, List<String>> params = splitQuery(new URI(request.uri()));
      assert params.containsKey("randao_reveal");
      assert params.get("randao_reveal").size() == 1;
      assert params.containsKey("slot");
      assert params.get("slot").size() == 1;
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(params.get("slot").get(0)));
      BLSSignature randaoReveal =
          BLSSignature.wrap(Bytes96.fromHexStringStrict(params.get("randao_reveal").get(0)));
      BeaconBlock.Builder builder =
          validatorDutiesService.prepareBlock(slot, randaoReveal, observableBeaconState);
      return mapper.writeValueAsString(BlockDataToBlock.serialize(builder.build()));
    } catch (AssertionError
        | UnsupportedEncodingException
        | URISyntaxException
        | IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Mono<Optional<Throwable>> acceptBlockSubmit(HttpServerRequest request) {
    return request
        .receive()
        .aggregate()
        .asString()
        .map(
            json -> {
              try {
                BeaconBlock block =
                    BlockDataToBlock.deserialize(
                        mapper.readValue(json, BlockSubmit.class).getBeaconBlock());
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
            });
  }

  private String produceAttestationResponse(HttpServerRequest request) {
    try {
      Map<String, List<String>> params = splitQuery(new URI(request.uri()));
      assert params.containsKey("validator_pubkey");
      assert params.get("validator_pubkey").size() == 1;
      assert params.containsKey("poc_bit");
      assert params.get("poc_bit").size() == 1;
      assert params.containsKey("slot");
      assert params.get("slot").size() == 1;
      assert params.containsKey("shard");
      assert params.get("shard").size() == 1;
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(params.get("slot").get(0)));
      BLSPubkey validatorPubkey =
          BLSPubkey.wrap(Bytes48.fromHexStringStrict(params.get("validator_pubkey").get(0)));
      Long pocBit =
          Long.valueOf(params.get("poc_bit").get(0)); // XXX: Proof of custody is a stub at Phase 0
      ShardNumber shard = ShardNumber.of(UInt64.valueOf(params.get("shard").get(0)));

      ValidatorIndex validatorIndex =
          spec.get_validator_index_by_pubkey(
              observableBeaconState.getLatestSlotState().createMutableCopy(), validatorPubkey);
      Attestation attestation =
          validatorDutiesService.prepareAttestation(
              validatorIndex, shard, observableBeaconState, slot);
      IndexedAttestation indexedAttestation =
          spec.convert_to_indexed(
              observableBeaconState.getLatestSlotState().createMutableCopy(), attestation);
      return mapper.writeValueAsString(
          BlockDataToBlock.presentIndexedAttestation(indexedAttestation));
    } catch (AssertionError
        | UnsupportedEncodingException
        | URISyntaxException
        | IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Mono<Optional<Throwable>> acceptAttestationSubmit(HttpServerRequest request) {
    return request
        .receive()
        .aggregate()
        .asString()
        .map(
            json -> {
              try {
                IndexedAttestation indexedAttestation =
                    BlockDataToBlock.parseIndexedAttestation(
                        mapper.readValue(json, AttestationSubmit.class).getAttestation());
                // Verification
                MutableBeaconState state =
                    observableBeaconState.getLatestSlotState().createMutableCopy();
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
                        bitfield,
                        indexedAttestation.getData(),
                        bitfield,
                        indexedAttestation.getSignature());
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
            });
  }

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> wrapJsonSupplier(
      Supplier<String> response) {
    return (httpServerRequest, httpServerResponse) ->
        httpServerResponse
            .addHeader("Content-type", "application/json")
            .sendString(Mono.just(response.get()));
  }

  /**
   * Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 202 ACCEPTED when catches {@link PartiallyFailedException} from func function
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   *
   * <p>Responses with 503 SERVICE UNAVAILABLE when not in short sync mode
   */
  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> acceptPostData(
      Function<HttpServerRequest, Mono<Optional<Throwable>>> func) {
    return (httpServerRequest, httpServerResponse) -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        return httpServerResponse.status(HttpResponseStatus.SERVICE_UNAVAILABLE).send();
      }

      return func.apply(httpServerRequest)
          .flatMap(
              throwable -> {
                if (!throwable.isPresent()) {
                  return httpServerResponse.status(HttpResponseStatus.ACCEPTED).send();
                } else {
                  Throwable ex = throwable.get();
                  if (ex instanceof PartiallyFailedException) {
                    return httpServerResponse.status(HttpResponseStatus.ACCEPTED).send();
                  } else if (ex instanceof InvalidInputException) {
                    return httpServerResponse.status(HttpResponseStatus.BAD_REQUEST).send();
                  } else if (ex instanceof NotAcceptableException) {
                    return httpServerResponse.status(HttpResponseStatus.NOT_ACCEPTABLE).send();
                  } else {
                    return httpServerResponse
                        .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                        .send();
                  }
                }
              });
    };
  }

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> wrapJsonFunction(
      Function<HttpServerRequest, String> response) {
    return (httpServerRequest, httpServerResponse) -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        return httpServerResponse.status(HttpResponseStatus.SERVICE_UNAVAILABLE).send();
      }

      try {
        return httpServerResponse
            .addHeader("Content-type", "application/json")
            .sendString(Mono.just(response.apply(httpServerRequest)));
      } catch (NotAcceptableInputException ex) {
        return httpServerResponse.status(HttpResponseStatus.NOT_ACCEPTABLE).send();
      } catch (InvalidInputException ex) {
        return httpServerResponse.status(HttpResponseStatus.BAD_REQUEST).send();
      }
    };
  }

  private String produceGenesisTimeResponse() {
    if (timeResponse == null) {
      throw new RuntimeException("Genesis time is not yet known!");
    }
    try {
      return mapper.writeValueAsString(timeResponse);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void shutdown() {
    server.disposeNow();
    serverRes.dispose();
  }
}
