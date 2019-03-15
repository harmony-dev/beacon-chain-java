package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpecHelpersData {
  @JsonProperty("bls-verify")
  private boolean blsVerifyEnabled = true;

  @JsonProperty("bls-sign")
  private boolean blsSignEnabled = true;

  @JsonProperty("verify-proof-of-possession")
  private boolean proofVerifyEnabled = true;


  public boolean isBlsVerifyEnabled() {
    return blsVerifyEnabled;
  }

  public void setBlsVerifyEnabled(boolean blsVerifyEnabled) {
    this.blsVerifyEnabled = blsVerifyEnabled;
  }

  public boolean isBlsSignEnabled() {
    return blsSignEnabled;
  }

  public void setBlsSignEnabled(boolean blsSignEnabled) {
    this.blsSignEnabled = blsSignEnabled;
  }

  public boolean isProofVerifyEnabled() {
    return proofVerifyEnabled;
  }

  public void setProofVerifyEnabled(boolean proofVerifyEnabled) {
    this.proofVerifyEnabled = proofVerifyEnabled;
  }
}
