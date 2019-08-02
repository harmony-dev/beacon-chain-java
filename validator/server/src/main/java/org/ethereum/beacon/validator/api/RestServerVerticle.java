package org.ethereum.beacon.validator.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import org.ethereum.beacon.validator.api.controller.ControllerRoute;

import java.util.Collection;

public class RestServerVerticle extends AbstractVerticle {
  private final Integer serverPort;
  private final Collection<ControllerRoute> controllers;

  public RestServerVerticle(Integer serverPort, Collection<ControllerRoute> controllers) {
    this.serverPort = serverPort;
    this.controllers = controllers;
  }

  @Override
  public void start() {
    Router router = Router.router(vertx);
    controllers.forEach(
        controller -> {
          router.get(controller.getPath()).handler(controller.getController().getHandler());
          router.post(controller.getPath()).handler(controller.getController().postHandler());
        });
    vertx.createHttpServer().requestHandler(router).listen(serverPort);
  }

  public Integer getServerPort() {
    return serverPort;
  }
}
