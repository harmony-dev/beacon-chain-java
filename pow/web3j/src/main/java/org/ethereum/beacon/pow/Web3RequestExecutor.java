package org.ethereum.beacon.pow;

import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class Web3RequestExecutor {
  private final Logger logger = LogManager.getLogger(Web3RequestExecutor.class);
  private final Web3j web3j;

  private static final Integer TIMEOUT_MS = 5 * 1000;
  private static final Integer REPEAT_INTERVAL_MS = 3 * 1000;
  private static final Integer  SYNC_THRESHOLD = 1000 * 60 * 3;

  public Web3RequestExecutor(Web3j web3j) {
    this.web3j = web3j;
  }

  private boolean isReady() {
    CompletableFuture<Boolean> notReadySyncing = new CompletableFuture<>();
    CompletableFuture<Void> notSyncing = new CompletableFuture<>();
    web3j
        .ethSyncing()
        .sendAsync()
        .thenAccept(
            ethSyncing -> {
              if (ethSyncing.isSyncing()) {
                notReadySyncing.complete(false);
              } else {
                notSyncing.complete(null);
              }
            });
    CompletableFuture<Boolean> syncingNotStalled =
        notSyncing
            .thenComposeAsync(
                aVoid ->
                    web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).sendAsync())
            .thenApply(
                ethBlock -> {
                  long timestamp = ethBlock.getBlock().getTimestamp().longValueExact() * 1000;
                  return System.currentTimeMillis() - SYNC_THRESHOLD < timestamp;
                });

    try {
      return notReadySyncing
          .applyToEither(syncingNotStalled, java.util.function.Function.identity())
          .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.debug("Unable to get isReady peer status because of exception", e);
      return false;
    }
  }

  void executeOnSyncDone(Runnable runnable) {
    CompletableFuture<Boolean> finished = new CompletableFuture<>();
    Schedulers.single()
        .scheduleDirect(
            () -> {
              while (!finished.isDone()) {
                try {
                  if (isReady()) {
                    runnable.run();
                    finished.complete(true);
                  }
                  Thread.sleep(REPEAT_INTERVAL_MS);
                } catch (InterruptedException e) {
                  logger.error("Sync task was interrupted by exception", e);
                }
              }
            });
  }
}
