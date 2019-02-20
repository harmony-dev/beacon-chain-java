package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.DepositContractParameters;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.uint.UInt64;

public class DepositContractParametersData implements DepositContractParameters {

  @JsonProperty("DEPOSIT_CONTRACT_ADDRESS")
  private String DEPOSIT_CONTRACT_ADDRESS;
  @JsonProperty("DEPOSIT_CONTRACT_TREE_DEPTH")
  private String DEPOSIT_CONTRACT_TREE_DEPTH;
  @JsonProperty("MIN_DEPOSIT_AMOUNT")
  private String MIN_DEPOSIT_AMOUNT;
  @JsonProperty("MAX_DEPOSIT_AMOUNT")
  private String MAX_DEPOSIT_AMOUNT;

  @Override
  @JsonIgnore
  public Address getDepositContractAddress() {
    return Address.fromHexString(getDEPOSIT_CONTRACT_ADDRESS());
  }

  @Override
  @JsonIgnore
  public UInt64 getDepositContractTreeDepth() {
    return UInt64.valueOf(getDEPOSIT_CONTRACT_TREE_DEPTH());
  }

  @Override
  @JsonIgnore
  public Gwei getMinDepositAmount() {
    return Gwei.castFrom(UInt64.valueOf(getMIN_DEPOSIT_AMOUNT()));
  }

  @Override
  @JsonIgnore
  public Gwei getMaxDepositAmount() {
    return Gwei.castFrom(UInt64.valueOf(getMAX_DEPOSIT_AMOUNT()));
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getDEPOSIT_CONTRACT_ADDRESS() {
    return DEPOSIT_CONTRACT_ADDRESS;
  }

  public void setDEPOSIT_CONTRACT_ADDRESS(String DEPOSIT_CONTRACT_ADDRESS) {
    this.DEPOSIT_CONTRACT_ADDRESS = DEPOSIT_CONTRACT_ADDRESS;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getDEPOSIT_CONTRACT_TREE_DEPTH() {
    return DEPOSIT_CONTRACT_TREE_DEPTH;
  }

  public void setDEPOSIT_CONTRACT_TREE_DEPTH(String DEPOSIT_CONTRACT_TREE_DEPTH) {
    this.DEPOSIT_CONTRACT_TREE_DEPTH = DEPOSIT_CONTRACT_TREE_DEPTH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_DEPOSIT_AMOUNT() {
    return MIN_DEPOSIT_AMOUNT;
  }

  public void setMIN_DEPOSIT_AMOUNT(String MIN_DEPOSIT_AMOUNT) {
    this.MIN_DEPOSIT_AMOUNT = MIN_DEPOSIT_AMOUNT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_DEPOSIT_AMOUNT() {
    return MAX_DEPOSIT_AMOUNT;
  }

  public void setMAX_DEPOSIT_AMOUNT(String MAX_DEPOSIT_AMOUNT) {
    this.MAX_DEPOSIT_AMOUNT = MAX_DEPOSIT_AMOUNT;
  }
}
