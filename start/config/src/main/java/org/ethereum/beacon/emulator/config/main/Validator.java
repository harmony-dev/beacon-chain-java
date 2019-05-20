package org.ethereum.beacon.emulator.config.main;

import org.ethereum.beacon.emulator.config.main.conract.Contract;

/** Validator settings */
public class Validator {
  private Contract contract;
  private Signer signer;

  public Contract getContract() {
    return contract;
  }

  public void setContract(Contract contract) {
    this.contract = contract;
  }

  public Signer getSigner() {
    return signer;
  }

  public void setSigner(Signer signer) {
    this.signer = signer;
  }
}
