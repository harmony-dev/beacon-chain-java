package org.ethereum.beacon.pow.validator;

/**
 * Registers validator in DepositContract and passes all way to active validator according to spec
 * <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/validator/0_beacon-chain-validator.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/validator/0_beacon-chain-validator.md</a>
 */
public interface ValidatorRegistrationService {
  void start();

  enum RegistrationStage {
    SEND_TX, // Send Deposit in Eth1
    AWAIT_INCLUSION, // Await processing of DepositContract root with our validator by smb
    AWAIT_ACTIVATION, // Await activation epoch
    VALIDATOR_START, // Start validator proposer / attestation services
    COMPLETE // We are over
  }
}
