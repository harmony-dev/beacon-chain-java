package org.ethereum.beacon.emulator.config.chainspec;

public class SpecHelpersOptions {
  private boolean blsVerifyEnabled = true;

  public boolean isBlsVerifyEnabled() {
    return blsVerifyEnabled;
  }

  public void setBlsVerifyEnabled(boolean blsVerifyEnabled) {
    this.blsVerifyEnabled = blsVerifyEnabled;
  }
}
