package org.ethereum.beacon.validator;

import org.ethereum.beacon.randao.Randao;

/** Validator registration within a PoW deposit contract. */
public class PoWValidatorRegistrar implements ValidatorRegistrar {

  @Override
  public boolean register(ValidatorCredentials credentials, Randao randao) {
    return false;
  }

  @Override
  public boolean isRegistered(ValidatorCredentials credentials) {
    return false;
  }
}
