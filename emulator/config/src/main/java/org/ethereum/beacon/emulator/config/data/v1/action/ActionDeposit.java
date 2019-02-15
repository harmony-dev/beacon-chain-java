package org.ethereum.beacon.emulator.config.data.v1.action;

public class ActionDeposit extends Action {
  private String creator;
  private String sender;
  private Long gasLimit;
  private String eth1From;
  private String eth1PrivKey;
  private String withdrawalCredentials;
  private Long amount;

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public Long getGasLimit() {
    return gasLimit;
  }

  public void setGasLimit(Long gasLimit) {
    this.gasLimit = gasLimit;
  }

  public String getEth1From() {
    return eth1From;
  }

  public void setEth1From(String eth1From) {
    this.eth1From = eth1From;
  }

  public String getEth1PrivKey() {
    return eth1PrivKey;
  }

  public void setEth1PrivKey(String eth1PrivKey) {
    this.eth1PrivKey = eth1PrivKey;
  }

  public String getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public void setWithdrawalCredentials(String withdrawalCredentials) {
    this.withdrawalCredentials = withdrawalCredentials;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }
}
