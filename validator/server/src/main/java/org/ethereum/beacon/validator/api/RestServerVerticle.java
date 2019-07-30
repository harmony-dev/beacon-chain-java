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
          switch (controller.getRequestType()) {
            case GET:
              {
                router.get(controller.getPath()).handler(controller.getController().getHandler());
                break;
              }
            case POST:
              {
                router.post(controller.getPath()).handler(controller.getController().getHandler());
                break;
              }
            default:
              {
                throw new RuntimeException(
                    String.format(
                        "Request type %s is not yet supported for controller registration",
                        controller.getRequestType()));
              }
          }
        });
    vertx.createHttpServer().requestHandler(router).listen(serverPort);
  }

  public Integer getServerPort() {
    return serverPort;
  }
}
