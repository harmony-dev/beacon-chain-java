package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.RewardAndPenaltyQuotients;
import tech.pegasys.artemis.util.uint.UInt64;

public class RewardAndPenaltyQuotientsData implements RewardAndPenaltyQuotients {

  @JsonProperty(value = "BASE_REWARD_QUOTIENT", access = JsonProperty.Access.WRITE_ONLY)
  private String BASE_REWARD_QUOTIENT;
  @JsonProperty(value = "WHISTLEBLOWER_REWARD_QUOTIENT", access = JsonProperty.Access.WRITE_ONLY)
  private String WHISTLEBLOWER_REWARD_QUOTIENT;
  @JsonProperty(value = "INCLUDER_REWARD_QUOTIENT", access = JsonProperty.Access.WRITE_ONLY)
  private String INCLUDER_REWARD_QUOTIENT;
  @JsonProperty(value = "INACTIVITY_PENALTY_QUOTIENT", access = JsonProperty.Access.WRITE_ONLY)
  private String INACTIVITY_PENALTY_QUOTIENT;

  @Override
  @JsonIgnore
  public UInt64 getBaseRewardQuotient() {
    return UInt64.valueOf(getBASE_REWARD_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getWhistleblowerRewardQuotient() {
    return UInt64.valueOf(getWHISTLEBLOWER_REWARD_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getIncluderRewardQuotient() {
    return UInt64.valueOf(getINCLUDER_REWARD_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getInactivityPenaltyQuotient() {
    return UInt64.valueOf(getINACTIVITY_PENALTY_QUOTIENT());
  }

  public String getBASE_REWARD_QUOTIENT() {
    return BASE_REWARD_QUOTIENT;
  }

  public void setBASE_REWARD_QUOTIENT(String BASE_REWARD_QUOTIENT) {
    this.BASE_REWARD_QUOTIENT = BASE_REWARD_QUOTIENT;
  }

  public String getWHISTLEBLOWER_REWARD_QUOTIENT() {
    return WHISTLEBLOWER_REWARD_QUOTIENT;
  }

  public void setWHISTLEBLOWER_REWARD_QUOTIENT(String WHISTLEBLOWER_REWARD_QUOTIENT) {
    this.WHISTLEBLOWER_REWARD_QUOTIENT = WHISTLEBLOWER_REWARD_QUOTIENT;
  }

  public String getINCLUDER_REWARD_QUOTIENT() {
    return INCLUDER_REWARD_QUOTIENT;
  }

  public void setINCLUDER_REWARD_QUOTIENT(String INCLUDER_REWARD_QUOTIENT) {
    this.INCLUDER_REWARD_QUOTIENT = INCLUDER_REWARD_QUOTIENT;
  }

  public String getINACTIVITY_PENALTY_QUOTIENT() {
    return INACTIVITY_PENALTY_QUOTIENT;
  }

  public void setINACTIVITY_PENALTY_QUOTIENT(String INACTIVITY_PENALTY_QUOTIENT) {
    this.INACTIVITY_PENALTY_QUOTIENT = INACTIVITY_PENALTY_QUOTIENT;
  }
}
