package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.RewardAndPenaltyQuotients;
import tech.pegasys.artemis.util.uint.UInt64;

public class RewardAndPenaltyQuotientsData implements RewardAndPenaltyQuotients {

  @JsonProperty("BASE_REWARD_FACTOR")
  private String BASE_REWARD_FACTOR;
  @JsonProperty("WHISTLEBLOWING_REWARD_QUOTIENT")
  private String WHISTLEBLOWING_REWARD_QUOTIENT;
  @JsonProperty("PROPOSER_REWARD_QUOTIENT")
  private String PROPOSER_REWARD_QUOTIENT;
  @JsonProperty("INACTIVITY_PENALTY_QUOTIENT")
  private String INACTIVITY_PENALTY_QUOTIENT;
  @JsonProperty("MIN_SLASHING_PENALTY_QUOTIENT")
  private String MIN_SLASHING_PENALTY_QUOTIENT;

  @Override
  @JsonIgnore
  public UInt64 getBaseRewardFactor() {
    return UInt64.valueOf(getBASE_REWARD_FACTOR());
  }

  @Override
  @JsonIgnore
  public UInt64 getWhistleblowingRewardQuotient() {
    return UInt64.valueOf(getWHISTLEBLOWING_REWARD_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getProposerRewardQuotient() {
    return UInt64.valueOf(getPROPOSER_REWARD_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getInactivityPenaltyQuotient() {
    return UInt64.valueOf(getINACTIVITY_PENALTY_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public UInt64 getMinSlashingPenaltyQuotient() {
    return UInt64.valueOf(getMIN_SLASHING_PENALTY_QUOTIENT());
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getBASE_REWARD_FACTOR() {
    return BASE_REWARD_FACTOR;
  }

  public void setBASE_REWARD_FACTOR(String BASE_REWARD_FACTOR) {
    this.BASE_REWARD_FACTOR = BASE_REWARD_FACTOR;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getWHISTLEBLOWING_REWARD_QUOTIENT() {
    return WHISTLEBLOWING_REWARD_QUOTIENT;
  }

  public void setWHISTLEBLOWING_REWARD_QUOTIENT(String WHISTLEBLOWING_REWARD_QUOTIENT) {
    this.WHISTLEBLOWING_REWARD_QUOTIENT = WHISTLEBLOWING_REWARD_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getPROPOSER_REWARD_QUOTIENT() {
    return PROPOSER_REWARD_QUOTIENT;
  }

  public void setPROPOSER_REWARD_QUOTIENT(String PROPOSER_REWARD_QUOTIENT) {
    this.PROPOSER_REWARD_QUOTIENT = PROPOSER_REWARD_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getINACTIVITY_PENALTY_QUOTIENT() {
    return INACTIVITY_PENALTY_QUOTIENT;
  }

  public void setINACTIVITY_PENALTY_QUOTIENT(String INACTIVITY_PENALTY_QUOTIENT) {
    this.INACTIVITY_PENALTY_QUOTIENT = INACTIVITY_PENALTY_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_SLASHING_PENALTY_QUOTIENT() {
    return MIN_SLASHING_PENALTY_QUOTIENT;
  }

  public void setMIN_SLASHING_PENALTY_QUOTIENT(String MIN_SLASHING_PENALTY_QUOTIENT) {
    this.MIN_SLASHING_PENALTY_QUOTIENT = MIN_SLASHING_PENALTY_QUOTIENT;
  }
}
