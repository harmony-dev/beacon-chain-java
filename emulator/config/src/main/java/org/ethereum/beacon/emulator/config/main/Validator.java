package org.ethereum.beacon.emulator.config.main;

import java.util.Map;

/** Validator settings */
public class Validator {
  private Map<String, String> contract;
  private Signer signer;

  public Map<String, String> getContract() {
    return contract;
  }

  public void setContract(Map<String, String> contract) {
    this.contract = contract;
  }

  public Signer getSigner() {
    return signer;
  }

  public void setSigner(Signer signer) {
    this.signer = signer;
  }
}
