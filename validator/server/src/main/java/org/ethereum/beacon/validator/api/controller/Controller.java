package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface Controller {
  /* Request handler */
  Handler<RoutingContext> getHandler();
}
