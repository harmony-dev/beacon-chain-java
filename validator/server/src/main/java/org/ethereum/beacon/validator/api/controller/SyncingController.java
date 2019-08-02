package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.stream.RxUtil;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.wire.PeerManager;
import org.ethereum.beacon.wire.sync.SyncManager;

public class SyncingController extends RestController {
  private static final Logger logger = LogManager.getLogger(SyncingController.class);
  private SyncingResponse syncingResponse = null;

  public SyncingController(SyncManager syncManager, PeerManager peerManager) {
    RxUtil.combineLatest(
            syncManager.getIsSyncingStream(),
            syncManager.getStartSlotStream(),
            syncManager.getLastSlotStream(),
            peerManager.getMaxSlotStream(),
            SyncingResponse::create)
        .doOnNext(this::updateResponse)
        .onErrorContinue((t, o) -> logger.error("Unexpected error: ", t))
        .subscribe();
  }

  private synchronized void updateResponse(SyncingResponse syncingResponse) {
    this.syncingResponse = syncingResponse;
  }

  private synchronized Object produceSyncingResponse() {
    return syncingResponse;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doJsonResponse(this::produceSyncingResponse);
  }
}
