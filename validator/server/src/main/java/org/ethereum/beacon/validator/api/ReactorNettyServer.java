package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import tech.pegasys.artemis.util.uint.UInt64;

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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReactorNettyServer implements RestServer {

  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 1234;
  private final BeaconChainSpec spec;
  private DisposableServer server;
  private ObjectMapper mapper = new ObjectMapper();
  private VersionResponse versionResponse = null;
  private TimeResponse timeResponse = null;
  private SyncingResponse syncingResponse = null;
  private boolean shortSync = false;
  private BeaconStateEx latestState = null;

  public ReactorNettyServer(
      BeaconChainSpec spec, ObservableStateProcessor stateProcessor, SyncManager syncManager) {
    this.spec = spec;
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.latestState = observableBeaconState.getLatestSlotState();
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

    HttpServer httpServer = HttpServer.create().host(SERVER_HOST).port(SERVER_PORT);
    final HttpServer serverWithRoutes = addRoutes(httpServer);

    CountDownLatch waitForStart = new CountDownLatch(1);
    Schedulers.createDefault()
        .newSingleThreadDaemon("api-server")
        .executeR(
            () -> {
              this.server = serverWithRoutes.bindNow();
              waitForStart.countDown();
              server.onDispose().block();
            });
    try {
      waitForStart.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /** TODO: move to utils Copied from https://stackoverflow.com/a/13592567 */
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

  // TODO: move to utils
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
                .get("/validator/duties", wrapJsonFunction(this::produceValidatorDutiesResponse)));
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

  private String produceValidatorDutiesResponse(HttpServerRequest request) {
    try {
      Map<String, List<String>> params = splitQuery(new URI(request.uri()));
      assert params.containsKey("validator_pubkeys");
      assert params.containsKey("epoch");
      assert params.get("epoch").size() == 1;
      EpochNumber epoch = EpochNumber.castFrom(UInt64.valueOf(params.get("epoch").get(0)));
      List<BLSPubkey> pubKeys =
          params.get("validator_pubkeys").stream()
              .map(BLSPubkey::fromHexString)
              .collect(Collectors.toList());
      Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> validatorDuties =
          spec.get_validator_duties_for_epoch(latestState, epoch);

      List<ValidatorDutiesResponse.ValidatorDuty> responseList = new ArrayList<>();
      for (BLSPubkey pubkey : pubKeys) {
        ValidatorIndex validatorIndex = spec.get_validator_index_by_pubkey(latestState, pubkey);
        ValidatorDutiesResponse.ValidatorDuty duty = new ValidatorDutiesResponse.ValidatorDuty();
        boolean attesterFound = false;
        boolean proposerFound = false;
        for (Map.Entry<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> entry :
            validatorDuties.entrySet()) {
          if (!proposerFound && entry.getValue().getValue0().equals(validatorIndex)) {
            duty.setBlockProposalSlot(entry.getKey().toBI());
            proposerFound = true;
          }

          if (!attesterFound) {
            for (ShardCommittee shardCommittee : entry.getValue().getValue1()) {
              if (shardCommittee.getCommittee().contains(validatorIndex)) {
                duty.setAttestationShard(shardCommittee.getShard().intValue());
                duty.setAttestationSlot(entry.getKey().toBI());
                attesterFound = true;
                break;
              }
            }
          }

          if (proposerFound && attesterFound) {
            break;
          }
        }

        if (duty.getAttestationSlot() != null) {
          duty.setValidatorPubkey(pubkey.toHexString());
          responseList.add(duty);
        }
      }
      // TODO: 406 Duties cannot be provided for the requested epoch. What epochs?

      return mapper.writeValueAsString(new ValidatorDutiesResponse(responseList));
    } catch (AssertionError | UnsupportedEncodingException | URISyntaxException ex) {
      throw new InvalidRequestSyntaxException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> wrapJsonSupplier(
      Supplier<String> response) {
    return (httpServerRequest, httpServerResponse) ->
        httpServerResponse
            .addHeader("Content-type", "application/json")
            .sendString(Mono.just(response.get()));
  }

  /**
   * Executes response function to produce reponse JSON and adds apropriate headers to the response
   *
   * <p>Responses with 503 SERVICE UNAVAILABLE when not in short sync mode
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidRequestSyntaxException} from
   * response function
   */
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
      } catch (InvalidRequestSyntaxException ex) {
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
  }
}
