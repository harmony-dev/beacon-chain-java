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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST service for beacon chain validator according to <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml">https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/validator/beacon_node_oapi.yaml</a>
 */
public class ValidatorRest extends ReactorNettyServer {

  private final BeaconChainSpec spec;
  private final WireApiSub wireApiSub;
  private final MutableBeaconChain beaconChain;
  private final ValidatorDutiesService validatorDutiesService;
  private final UInt64 chainId;

  private ObjectMapper mapper = new ObjectMapper();
  private String version = null;
  private Time time = null;
  private SyncingResponse syncingResponse = null;
  private boolean shortSync = false;
  private ObservableBeaconState observableBeaconState = null;

  public ValidatorRest(
      String serverHost,
      Integer serverPort,
      BeaconChainSpec spec,
      ObservableStateProcessor stateProcessor,
      SyncManager syncManager,
      UInt64 chainId,
      ValidatorDutiesService validatorDutiesService,
      WireApiSub wireApiSub,
      MutableBeaconChain beaconChain) {
    super(serverHost, serverPort);
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

  @Override
  HttpServer addRoutes(HttpServer httpServer) {
    return httpServer.route(
        routes ->
            routes
                .get("/node/version", doJsonResponse(this::produceVersionResponse))
                .get("/node/genesis_time", doJsonResponse(this::produceGenesisTimeResponse))
                .get("/node/syncing", doJsonResponse(this::produceSyncingResponse))
                .get(
                    "/validator/duties",
                    processGetRequestImpl(this::produceValidatorDutiesResponse))
                .get("/validator/block", processGetRequestImpl(this::produceValidatorBlockResponse))
                .post("/validator/block", processPostRequestImpl(this::acceptBlockSubmit))
                .get(
                    "/validator/attestation",
                    processGetRequestImpl(this::produceAttestationResponse))
                .post(
                    "/validator/attestation", processPostRequestImpl(this::acceptAttestationSubmit))
                .get("/node/fork", doJsonResponse(this::produceForkResponse)));
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
      Map<String, List<String>> params = getRequestParams(request);
      EpochNumber epoch;
      BeaconStateEx stateEx = observableBeaconState.getLatestSlotState();
      if (params.containsKey("epoch")) {
        epoch = EpochNumber.castFrom(UInt64.valueOf(params.get("epoch").get(0)));
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

      return mapper.writeValueAsString(new ValidatorDutiesResponse(responseList));
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (InvalidInputException | NotAcceptableInputException ex) {
      throw ex;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String produceValidatorBlockResponse(HttpServerRequest request) {
    try {
      Map<String, List<String>> params = getRequestParams(request);
      SlotNumber slot = SlotNumber.castFrom(UInt64.valueOf(getParamString("slot", params)));
      BLSSignature randaoReveal =
          BLSSignature.wrap(Bytes96.fromHexStringStrict(getParamString("randao_reveal", params)));
      BeaconBlock.Builder builder =
          validatorDutiesService.prepareBlock(slot, randaoReveal, observableBeaconState);
      return mapper.writeValueAsString(BeaconBlockConverter.serialize(builder.build()));
    } catch (IllegalArgumentException ex) {
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
                BeaconBlock block = mapper.readValue(json, BlockSubmit.class).createBeaconBlock();
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
      Map<String, List<String>> params = getRequestParams(request);
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
              validatorIndex, shard, observableBeaconState, slot);
      IndexedAttestation indexedAttestation =
          spec.convert_to_indexed(
              observableBeaconState.getLatestSlotState().createMutableCopy(), attestation);
      return mapper.writeValueAsString(
          BeaconBlockConverter.presentIndexedAttestation(indexedAttestation));
    } catch (IllegalArgumentException ex) {
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
                    mapper.readValue(json, AttestationSubmit.class).createAttestation();
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

  private String produceGenesisTimeResponse() {
    if (time == null) {
      throw new RuntimeException("Genesis time is not yet known!");
    }
    return "" + time.getValue();
  }

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> processGetRequestImpl(
      Function<HttpServerRequest, String> response) {
    return (httpServerRequest, httpServerResponse) -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        return httpServerResponse.status(HttpResponseStatus.SERVICE_UNAVAILABLE).send();
      }
      return processGetRequest(httpServerRequest, httpServerResponse, response);
    };
  }

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> processPostRequestImpl(
      Function<HttpServerRequest, Mono<Optional<Throwable>>> func) {
    return (httpServerRequest, httpServerResponse) -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        return httpServerResponse.status(HttpResponseStatus.SERVICE_UNAVAILABLE).send();
      }
      return processPostRequest(httpServerRequest, httpServerResponse, func);
    };
  }
}
