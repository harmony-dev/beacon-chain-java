package org.ethereum.beacon.validator.api;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.resources.LoopResources;

import javax.ws.rs.NotAcceptableException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/** REST server based on Reactor Netty */
public abstract class ReactorNettyServer implements RestServer {

  private final DisposableServer server;
  private final LoopResources serverRes;

  public ReactorNettyServer(String serverHost, Integer serverPort) {
    serverRes = LoopResources.create("server");
    HttpServer httpServer =
        HttpServer.create()
            .tcpConfiguration(tcpServer -> tcpServer.runOn(serverRes))
            .host(serverHost)
            .port(serverPort);
    final HttpServer serverWithRoutes = addRoutes(httpServer);
    this.server = serverWithRoutes.bindNow();
  }

  /**
   * Parses params from request uri and returns key value map. Several values per key supported
   *
   * @param request Server request
   * @return GET query parameters ?key1=value1&key2=value2&key2=value3 -> key1: [value1], key2:
   *     [value2, value3]
   * @throws URISyntaxException when couldn't parse uri
   * @throws UnsupportedEncodingException when couldn't parse uri
   */
  static Map<String, List<String>> getRequestParams(HttpServerRequest request)
      throws InvalidInputException {
    try {
      return splitQuery(new URI(request.uri()));
    } catch (UnsupportedEncodingException | URISyntaxException ex) {
      throw new InvalidInputException("Couldn't parse query string");
    }
  }

  static String getParamString(String param, Map<String, List<String>> params)
      throws InvalidInputException {
    try {
      assert params.containsKey(param);
      assert params.get(param).size() == 1;
      return params.get(param).get(0);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain required param `%s`", param));
    }
  }

  static List<String> getParamStringList(String param, Map<String, List<String>> params)
      throws InvalidInputException {
    try {
      assert params.containsKey(param);
      return params.get(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain required param `%s`", param));
    }
  }

  /** Adopted from https://stackoverflow.com/a/13592567 */
  private static Map<String, List<String>> splitQuery(URI uri) throws UnsupportedEncodingException {
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

  /**
   * POST Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 202 ACCEPTED when catches {@link PartiallyFailedException} from func function
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   */
  static Publisher<Void> processPostRequest(
      HttpServerRequest httpServerRequest,
      HttpServerResponse httpServerResponse,
      Function<HttpServerRequest, Mono<Optional<Throwable>>> func) {

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
                  return httpServerResponse.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).send();
                }
              }
            });
  }

  /**
   * GET Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 202 ACCEPTED when catches {@link PartiallyFailedException} from func function
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   */
  static Publisher<Void> processGetRequest(
      HttpServerRequest httpServerRequest,
      HttpServerResponse httpServerResponse,
      Function<HttpServerRequest, String> response) {
    try {
      return httpServerResponse
          .addHeader("Content-type", "application/json")
          .sendString(Mono.just(response.apply(httpServerRequest)));
    } catch (NotAcceptableInputException ex) {
      return httpServerResponse.status(HttpResponseStatus.NOT_ACCEPTABLE).send();
    } catch (InvalidInputException ex) {
      return httpServerResponse.status(HttpResponseStatus.BAD_REQUEST).send();
    }
  }

  /** Override and init routes using httpServer.route(..! */
  abstract HttpServer addRoutes(HttpServer httpServer);

  /**
   * Produces json response using supplier
   *
   * @param response Json string supplier
   * @return request/response handling function
   */
  BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> doJsonResponse(
      Supplier<String> response) {
    return (httpServerRequest, httpServerResponse) ->
        httpServerResponse
            .addHeader("Content-type", "application/json")
            .sendString(Mono.just(response.get()));
  }

  @Override
  public void shutdown() {
    server.disposeNow();
    serverRes.dispose();
  }
}
