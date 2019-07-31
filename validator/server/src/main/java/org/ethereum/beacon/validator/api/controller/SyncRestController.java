package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.NotAcceptableInputException;
import org.ethereum.beacon.validator.api.PartiallyFailedException;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.function.Function;

public abstract class SyncRestController extends RestController {
  private boolean shortSync = false;

  public SyncRestController(SyncManager syncManager) {
    Flux.from(syncManager.getSyncModeStream())
        .map(s -> s.equals(SyncManager.SyncMode.Short))
        .subscribe(s -> shortSync = s);
  }

  /**
   * GET Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   *
   * <p>Responses with 406 NOT ACCEPTABLE when catches {@link NotAcceptableInputException} from func
   * function
   *
   * <p>Responses with 500 INTERNAL SERVER ERROR for all other errors
   */
  Handler<RoutingContext> processGetRequestImpl(Function<HttpServerRequest, Object> response) {
    return event -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        event.response().setStatusCode(503).end();
      } else {
        try {
          doJsonResponse(event, () -> response.apply(event.request()));
        } catch (InvalidInputException ex) {
          event.response().setStatusCode(400).end();
        } catch (NotAcceptableInputException ex) {
          event.response().setStatusCode(406).end();
        } catch (Exception ex) {
          event.response().setStatusCode(500).end();
        }
      }
    };
  }

  /**
   * POST Endpoint helper
   *
   * <p>Executes func with `HttpServerRequest` input and returns 200 OK if everything was ok
   *
   * <p>Responses with 202 ACCEPTED when catches {@link PartiallyFailedException} from func function
   *
   * <p>Responses with 400 BAD REQUEST when catches {@link InvalidInputException} from func function
   *
   * <p>Responses with 406 NOT ACCEPTABLE when catches {@link NotAcceptableInputException} from func
   * function
   *
   * <p>Responses with 500 INTERNAL SERVER ERROR for all other errors
   */
  Handler<RoutingContext> processPostRequestImpl(Function<String, Optional<Throwable>> func) {
    return event -> {
      if (!shortSync) {
        // 503 Beacon node is currently syncing, try again later.
        event.response().setStatusCode(503).end();
      } else {
        event
            .request()
            .bodyHandler(
                buffer -> {
                  Optional<Throwable> result = func.apply(buffer.toString());
                  if (!result.isPresent()) {
                    event.response().setStatusCode(200).end();
                  } else {
                    Throwable ex = result.get();
                    if (ex instanceof PartiallyFailedException) {
                      event.response().setStatusCode(202).end();
                    } else if (ex instanceof InvalidInputException) {
                      event.response().setStatusCode(400).end();
                    } else if (ex instanceof NotAcceptableInputException) {
                      event.response().setStatusCode(406).end();
                    } else {
                      event.response().setStatusCode(500).end();
                    }
                  }
                });
      }
    };
  }
}
