package org.ethereum.beacon.pow.validator;

import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Util for creating validator transactions {@see
 * https://github.com/ethereum/eth2.0-specs/blob/dev/specs/validator/0_beacon-chain-validator.md}
 */
public interface TransactionBuilder {
  boolean isReady();

  BytesValue createTransaction(String fromAddress, BytesValue depositInput, Gwei amount);

  BytesValue signTransaction(BytesValue unsignedTransaction, BytesValue eth1PrivKey);
}
