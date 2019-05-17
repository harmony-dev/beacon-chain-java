package org.ethereum.beacon.pow.validator;

import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

/** Util for creating validator transactions */
public interface TransactionBuilder {
  CompletableFuture<BytesValue> createTransaction(String fromAddress, DepositData depositData, Gwei amount);

  CompletableFuture<BytesValue> signTransaction(BytesValue unsignedTransaction, BytesValue eth1PrivKey);
}
