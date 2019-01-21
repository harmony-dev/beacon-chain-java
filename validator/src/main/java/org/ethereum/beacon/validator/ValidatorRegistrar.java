package org.ethereum.beacon.validator;

import org.ethereum.beacon.randao.Randao;

/** Registers validator by sending a deposit ot the PoW chain contract. */
public interface ValidatorRegistrar {

  /**
   * @param credentials
   * @return
   */
  boolean register(ValidatorCredentials credentials, Randao randao);

  boolean isRegistered(ValidatorCredentials credentials);
}
