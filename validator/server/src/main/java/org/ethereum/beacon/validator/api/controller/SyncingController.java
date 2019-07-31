package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;

public class SyncingController extends RestController {
  private SyncingResponse syncingResponse = null;

  public SyncingController(SyncManager syncManager) {
    Flux.from(syncManager.getSyncStatusStream())
        .subscribe(
            syncStatus -> {
              syncingResponse = SyncingResponse.fromManagerStatus(syncStatus);
            });
  }

  private Object produceSyncingResponse() {
    return syncingResponse;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doJsonResponse(this::produceSyncingResponse);
  }
}
