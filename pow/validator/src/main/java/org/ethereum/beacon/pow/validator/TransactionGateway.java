package org.ethereum.beacon.pow.validator;

import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

/** Gateway to Eth1 */
public interface TransactionGateway {
  boolean isReady();

  CompletableFuture<TxStatus> send(BytesValue signedTransaction);

  enum TxStatus {
    SUCCESS,
    ERROR
  }
}
