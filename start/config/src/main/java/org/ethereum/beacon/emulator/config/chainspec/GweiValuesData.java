package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.GweiValues;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.uint.UInt64;

public class GweiValuesData implements GweiValues {

  @JsonProperty("MIN_DEPOSIT_AMOUNT")
  private String MIN_DEPOSIT_AMOUNT;
  @JsonProperty("MAX_DEPOSIT_AMOUNT")
  private String MAX_DEPOSIT_AMOUNT;
  @JsonProperty("FORK_CHOICE_BALANCE_INCREMENT")
  private String FORK_CHOICE_BALANCE_INCREMENT;
  @JsonProperty("EJECTION_BALANCE")
  private String EJECTION_BALANCE;

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

  @Override
  @JsonIgnore
  public Gwei getForkChoiceBalanceIncrement() {
    return Gwei.castFrom(UInt64.valueOf(getFORK_CHOICE_BALANCE_INCREMENT()));
  }

  @Override
  @JsonIgnore
  public Gwei getEjectionBalance() {
    return Gwei.castFrom(UInt64.valueOf(getEJECTION_BALANCE()));
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

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getFORK_CHOICE_BALANCE_INCREMENT() {
    return FORK_CHOICE_BALANCE_INCREMENT;
  }

  public void setFORK_CHOICE_BALANCE_INCREMENT(String FORK_CHOICE_BALANCE_INCREMENT) {
    this.FORK_CHOICE_BALANCE_INCREMENT = FORK_CHOICE_BALANCE_INCREMENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEJECTION_BALANCE() {
    return EJECTION_BALANCE;
  }

  public void setEJECTION_BALANCE(String EJECTION_BALANCE) {
    this.EJECTION_BALANCE = EJECTION_BALANCE;
  }
}
