package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.GweiValues;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.uint.UInt64;

public class GweiValuesData implements GweiValues {

  @JsonProperty("MIN_DEPOSIT_AMOUNT")
  private String MIN_DEPOSIT_AMOUNT;
  @JsonProperty("MAX_EFFECTIVE_BALANCE")
  private String MAX_EFFECTIVE_BALANCE;
  @JsonProperty("EFFECTIVE_BALANCE_INCREMENT")
  private String EFFECTIVE_BALANCE_INCREMENT;
  @JsonProperty("EJECTION_BALANCE")
  private String EJECTION_BALANCE;

  @Override
  @JsonIgnore
  public Gwei getMinDepositAmount() {
    return Gwei.castFrom(UInt64.valueOf(getMIN_DEPOSIT_AMOUNT()));
  }

  @Override
  @JsonIgnore
  public Gwei getMaxEffectiveBalance() {
    return Gwei.castFrom(UInt64.valueOf(getMAX_EFFECTIVE_BALANCE()));
  }

  @Override
  @JsonIgnore
  public Gwei getEffectiveBalanceIncrement() {
    return Gwei.castFrom(UInt64.valueOf(getEFFECTIVE_BALANCE_INCREMENT()));
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
  public String getMAX_EFFECTIVE_BALANCE() {
    return MAX_EFFECTIVE_BALANCE;
  }

  public void setMAX_EFFECTIVE_BALANCE(String MAX_EFFECTIVE_BALANCE) {
    this.MAX_EFFECTIVE_BALANCE = MAX_EFFECTIVE_BALANCE;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEFFECTIVE_BALANCE_INCREMENT() {
    return EFFECTIVE_BALANCE_INCREMENT;
  }

  public void setEFFECTIVE_BALANCE_INCREMENT(String EFFECTIVE_BALANCE_INCREMENT) {
    this.EFFECTIVE_BALANCE_INCREMENT = EFFECTIVE_BALANCE_INCREMENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEJECTION_BALANCE() {
    return EJECTION_BALANCE;
  }

  public void setEJECTION_BALANCE(String EJECTION_BALANCE) {
    this.EJECTION_BALANCE = EJECTION_BALANCE;
  }
}
