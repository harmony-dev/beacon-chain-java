package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ReactorNettyServer implements RestServer {

  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 1234;
  private DisposableServer server;
  private ObjectMapper mapper =
      new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
  private VersionResponse versionResponse = null;
  private TimeResponse timeResponse = null;

  public ReactorNettyServer(ObservableStateProcessor stateProcessor) {
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              if (timeResponse == null) {
                timeResponse =
                    new TimeResponse(
                        observableBeaconState.getLatestSlotState().getGenesisTime().getValue());
              }
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
                .get("/node/version", wrapJsonProducer(this::produceVersionJson))
                .get("/node/genesis_time", wrapJsonProducer(this::produceGenesisTimeResponse))
        //                .post("/echo",
        //                    (request, response) -> response.send(request.receive().retain()))
        //                .get("/path/{param}",
        //                    (request, response) ->
        // response.sendString(Mono.just(request.param("param"))))
        );
  }

  private String produceVersionJson() {
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

  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> wrapJsonProducer(
      Supplier<String> response) {
    return (httpServerRequest, httpServerResponse) ->
        httpServerResponse
            .addHeader("Content-type", "application/json")
            .sendString(Mono.just(response.get()));
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
