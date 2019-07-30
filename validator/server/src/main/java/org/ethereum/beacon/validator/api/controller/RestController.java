package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.validator.api.InvalidInputException;

import java.util.List;
import java.util.function.Supplier;

public abstract class RestController implements Controller {
  static String getParamString(String param, MultiMap params) throws InvalidInputException {
    try {
      assert params.contains(param);
      assert params.getAll(param).size() == 1;
      return params.get(param);
    } catch (AssertionError e) {
      throw new InvalidInputException(
          String.format("Parameters map doesn't contain 1 required param `%s`", param));
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

  /**
   * Initiates json response using supplier to the context
   *
   * @param response Json string supplier
   */
  void doStringResponse(RoutingContext context, Supplier<String> response) {
    context
        .response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(response.get());
  }

  /**
   * Initiates json response using supplier of java object to the context
   *
   * @param response Json string supplier
   */
  void doJsonResponse(RoutingContext context, Supplier<Object> response) {
    doStringResponse(context, () -> Json.encodePrettily(response.get()));
  }

  /**
   * Produces json response using supplier
   *
   * @param response Json string supplier
   * @return request/response handling function
   */
  Handler<RoutingContext> doStringResponse(Supplier<String> response) {
    return event -> doStringResponse(event, response);
  }

  /**
   * Produces json response using supplier
   *
   * @param response Json string supplier
   * @return request/response handling function
   */
  Handler<RoutingContext> doJsonResponse(Supplier<Object> response) {
    return event -> doJsonResponse(event, response);
  }
}
