package org.ethereum.beacon.pow.validator;

import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Util for creating validator transactions */
public interface TransactionBuilder {
  boolean isReady();

  BytesValue createTransaction(String fromAddress, BytesValue depositInput, Gwei amount);

  BytesValue signTransaction(BytesValue unsignedTransaction, BytesValue eth1PrivKey);
}
