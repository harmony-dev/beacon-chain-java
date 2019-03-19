package org.ethereum.beacon.emulator.config.chainspec;

public class SpecHelpersData {
  private boolean blsVerify = true;

  private boolean blsVerifyProofOfPosession = true;

  private boolean blsSign = true;

  public boolean isBlsVerify() {
    return blsVerify;
  }

  public void setBlsVerify(boolean blsVerify) {
    this.blsVerify = blsVerify;
  }

  public boolean isBlsVerifyProofOfPosession() {
    return blsVerifyProofOfPosession;
  }

  public void setBlsVerifyProofOfPosession(boolean blsVerifyProofOfPosession) {
    this.blsVerifyProofOfPosession = blsVerifyProofOfPosession;
  }

  public boolean isBlsSign() {
    return blsSign;
  }

  public void setBlsSign(boolean blsSign) {
    this.blsSign = blsSign;
  }
}
