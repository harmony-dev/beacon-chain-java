package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.start.common.ClientInfo;

public class VersionController extends RestController {
  private String produceVersionResponse() {
    return ClientInfo.fullTitleVersion(VersionController.class);
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doStringResponse(this::produceVersionResponse);
  }
}
