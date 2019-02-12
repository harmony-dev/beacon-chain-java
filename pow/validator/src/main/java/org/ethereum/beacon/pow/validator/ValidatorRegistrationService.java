package org.ethereum.beacon.pow.validator;

import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.annotation.Nullable;

/**
 * Registers validator in DepositContract and runs all way to active validator according to the spec
 * <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/validator/0_beacon-chain-validator.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/validator/0_beacon-chain-validator.md</a>
 */
public interface ValidatorRegistrationService {
  void start(
      MessageSigner<BLSSignature> signer,
      BLSPubkey pubKey,
      @Nullable Hash32 withdrawalCredentials,
      @Nullable Gwei amount,
      @Nullable Address eth1From,
      @Nullable BytesValue eth1PrivKey);

  enum RegistrationStage {
    SEND_TX, // Send Deposit in Eth1
    AWAIT_INCLUSION, // Await processing of DepositContract root with our validator by smb
    AWAIT_ACTIVATION, // Await activation epoch
    VALIDATOR_START, // Start validator proposer / attestation services
    COMPLETE // We are over
  }
}