package org.ethereum.beacon.pow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.pow.validator.TransactionGateway;
import org.web3j.protocol.Web3j;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

public class Web3JTransactionGateway implements TransactionGateway {
  private static final Logger logger = LogManager.getLogger(Web3JTransactionGateway.class);
  private final Web3RequestExecutor web3RequestExecutor;
  private final Web3j web3j;

  public Web3JTransactionGateway(Web3j web3j) {
    this.web3j = web3j;
    this.web3RequestExecutor = new Web3RequestExecutor(web3j);
  }

  @Override
  public CompletableFuture<TxStatus> send(BytesValue signedTransaction) {
    CompletableFuture<TxStatus> result = new CompletableFuture<>();
    web3RequestExecutor.executeOnSyncDone(
        () -> {
          web3j
              .ethSendRawTransaction(signedTransaction.toString())
              .sendAsync()
              .thenAccept(
                  ethSendTransaction -> {
                    if (ethSendTransaction == null
                        || ethSendTransaction.getTransactionHash() == null) {
                      result.complete(TxStatus.ERROR);
                    } else {
                      result.complete(TxStatus.SUCCESS);
                    }
                  });
        });

    return result;
  }
}
