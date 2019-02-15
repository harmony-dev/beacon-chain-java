package org.ethereum.beacon.emulator.config.data.v1;

public class Signer {
  private SignerImplementation implementation;

  public SignerImplementation getImplementation() {
    return implementation;
  }

  public void setImplementation(SignerImplementation implementation) {
    this.implementation = implementation;
  }
}
