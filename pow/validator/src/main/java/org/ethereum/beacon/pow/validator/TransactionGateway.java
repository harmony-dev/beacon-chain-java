package org.ethereum.beacon.pow.validator;

import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

/** Gateway to Eth1, allows sending of signed transactions */
public interface TransactionGateway {
  CompletableFuture<TxStatus> send(BytesValue signedTransaction);

  enum TxStatus {
    SUCCESS,
    ERROR
  }
}
