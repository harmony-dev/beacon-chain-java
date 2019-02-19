package org.ethereum.beacon.pow;

import org.ethereum.beacon.pow.validator.TransactionGateway;
import org.ethereum.core.Transaction;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus;
import org.ethereum.listener.EthereumListenerAdapter;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EthereumJTransactionGateway implements TransactionGateway {
  private Ethereum ethereum;

  public EthereumJTransactionGateway(Ethereum ethereum) {
    this.ethereum = ethereum;
  }

  private boolean isReady() {
    return ethereum.getSyncStatus().getStage() != SyncStatus.SyncStage.Complete;
  }

  @Override
  public CompletableFuture<TxStatus> send(BytesValue signedTransaction) {
    CompletableFuture<TxStatus> result = new CompletableFuture<>();
    executeOnSyncDone(() -> {
      Future txFuture =
          ethereum.submitTransaction(new Transaction(signedTransaction.getArrayUnsafe()));
      try {
        if (txFuture.get() == null) {
          result.complete(TxStatus.ERROR);
        } else {
          result.complete(TxStatus.SUCCESS);
        }
      } catch (InterruptedException | ExecutionException e) {
        result.complete(TxStatus.ERROR);
      }
    });

    return result;
  }

  private void executeOnSyncDone(Runnable runnable) {
    if (isReady()) {
      runnable.run();
    } else {
      ethereum.addListener(
          new EthereumListenerAdapter() {
            @Override
            public void onSyncDone(SyncState state) {
              if (state == SyncState.COMPLETE) {
                runnable.run();
              }
            }
          });
    }
  }
}
