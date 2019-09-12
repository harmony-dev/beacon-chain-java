package org.ethereum.beacon.emulator.config.node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyData {
  private String privkey;
  @JsonProperty
  private String pubkey;

  public String getPrivkey() {
    return privkey;
  }

  public void setPrivkey(String privkey) {
    this.privkey = privkey;
  }

  public String getPubkey() {
    return pubkey;
  }

  public void setPubkey(String pubkey) {
    this.pubkey = pubkey;
  }
}
