package org.ethereum.beacon.emulator.config.main.conract;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EmulatorContract extends Contract {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT")
  private Date genesisTime;

  private Integer balance;
  private List<ValidatorKeys> keys;
  private String eth1BlockHash = createInteropEth1BlockHash();

  /**
   * @return HEX String representation of a hash32 consisting of 32 0x42 bytes, which is the interop
   *     default.
   */
  private static String createInteropEth1BlockHash() {
    byte ch = 0x42;
    byte[] res = new byte[32];
    Arrays.fill(res, ch);
    return Bytes32.wrap(res).toString();
  }

  public Date getGenesisTime() {
    return genesisTime;
  }

  public void setGenesisTime(Date genesisTime) {
    this.genesisTime = genesisTime;
  }

  public Integer getBalance() {
    return balance;
  }

  public void setBalance(Integer balance) {
    this.balance = balance;
  }

  public List<ValidatorKeys> getKeys() {
    return keys;
  }

  public void setKeys(List<ValidatorKeys> keys) {
    this.keys = keys;
  }

  public String getEth1BlockHash() {
    return eth1BlockHash;
  }

  public void setEth1BlockHash(String eth1BlockHash) {
    this.eth1BlockHash = eth1BlockHash;
  }
}
