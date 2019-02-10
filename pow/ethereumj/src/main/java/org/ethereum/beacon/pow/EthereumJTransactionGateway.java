package org.ethereum.beacon.pow;

import org.ethereum.beacon.pow.validator.IncompleteSyncException;
import org.ethereum.beacon.pow.validator.TransactionGateway;
import org.ethereum.core.Transaction;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EthereumJTransactionGateway implements TransactionGateway {
  private Ethereum ethereum;

  public EthereumJTransactionGateway(Ethereum ethereum) {
    this.ethereum = ethereum;
  }

  @Override
  public boolean isReady() {
    return ethereum.getSyncStatus().getStage() != SyncStatus.SyncStage.Complete;
  }

  @Override
  public CompletableFuture<TxStatus> send(BytesValue signedTransaction) {
    if (!(isReady())) {
      throw new IncompleteSyncException(
          "Unable to create transaction, because sync is not done yet. Query when sync is done.");
    }

    Future txFuture =
        ethereum.submitTransaction(new Transaction(signedTransaction.getArrayUnsafe()));
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            if (txFuture.get() == null) {
              return TxStatus.ERROR;
            } else {
              return TxStatus.SUCCESS;
            }
          } catch (InterruptedException | ExecutionException e) {
            return TxStatus.ERROR;
          }
        });
  }
}
