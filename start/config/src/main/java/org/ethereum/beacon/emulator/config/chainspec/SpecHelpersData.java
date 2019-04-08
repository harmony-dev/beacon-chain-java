package org.ethereum.beacon.emulator.config.chainspec;

public class SpecHelpersData {
  public static final SpecHelpersData DEFAULT = new SpecHelpersData();

  private boolean blsVerify = true;

  private boolean blsVerifyProofOfPossession = true;

  private boolean blsSign = true;

  public boolean isBlsVerify() {
    return blsVerify;
  }

  public void setBlsVerify(boolean blsVerify) {
    this.blsVerify = blsVerify;
  }

  public boolean isBlsVerifyProofOfPossession() {
    return blsVerifyProofOfPossession;
  }

  public void setBlsVerifyProofOfPossession(boolean blsVerifyProofOfPossession) {
    this.blsVerifyProofOfPossession = blsVerifyProofOfPossession;
  }

  public boolean isBlsSign() {
    return blsSign;
  }

  public void setBlsSign(boolean blsSign) {
    this.blsSign = blsSign;
  }
}
