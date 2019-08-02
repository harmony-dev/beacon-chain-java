package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface Controller {
  /* GET request handler */
  default Handler<RoutingContext> getHandler() {
    return event -> {
      event.response().setStatusCode(404).end();
    };
  }

  /* POST request handler */
  default Handler<RoutingContext> postHandler() {
    return event -> {
      event.response().setStatusCode(404).end();
    };
  }
}
