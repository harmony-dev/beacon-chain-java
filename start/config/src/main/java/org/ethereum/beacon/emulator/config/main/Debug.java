package org.ethereum.beacon.emulator.config.main;

public class Debug {
  private boolean logWireCipher;
  private boolean logWirePlain;
  private boolean logMuxFrames;
  private boolean logEthPubsub;
  private boolean logEthRpc;

  public boolean isLogWireCipher() {
    return logWireCipher;
  }

  public void setLogWireCipher(boolean logWireCipher) {
    this.logWireCipher = logWireCipher;
  }

  public boolean isLogWirePlain() {
    return logWirePlain;
  }

  public void setLogWirePlain(boolean logWirePlain) {
    this.logWirePlain = logWirePlain;
  }

  public boolean isLogMuxFrames() {
    return logMuxFrames;
  }

  public void setLogMuxFrames(boolean logMuxFrames) {
    this.logMuxFrames = logMuxFrames;
  }

  public boolean isLogEthPubsub() {
    return logEthPubsub;
  }

  public void setLogEthPubsub(boolean logEthPubsub) {
    this.logEthPubsub = logEthPubsub;
  }

  public boolean isLogEthRpc() {
    return logEthRpc;
  }

  public void setLogEthRpc(boolean logEthRpc) {
    this.logEthRpc = logEthRpc;
  }
}
