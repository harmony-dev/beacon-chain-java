package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.MiscParameters;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt64;

public class MiscParametersData implements MiscParameters {

  @JsonProperty("SHARD_COUNT")
  private String SHARD_COUNT;
  @JsonProperty("TARGET_COMMITTEE_SIZE")
  private String TARGET_COMMITTEE_SIZE;
  @JsonProperty("MAX_VALIDATORS_PER_COMMITTEE")
  private String MAX_VALIDATORS_PER_COMMITTEE;
  @JsonProperty("MIN_PER_EPOCH_CHURN_LIMIT")
  private String MIN_PER_EPOCH_CHURN_LIMIT;
  @JsonProperty("CHURN_LIMIT_QUOTIENT")
  private String CHURN_LIMIT_QUOTIENT;
  @JsonProperty("SHUFFLE_ROUND_COUNT")
  private String SHUFFLE_ROUND_COUNT;
  @JsonProperty("MIN_GENESIS_ACTIVE_VALIDATOR_COUNT")
  private String MIN_GENESIS_ACTIVE_VALIDATOR_COUNT;
  @JsonProperty("MIN_GENESIS_TIME")
  private String MIN_GENESIS_TIME;

  @Override
  @JsonIgnore
  public ShardNumber getShardCount() {
    return new ShardNumber(UInt64.valueOf(getSHARD_COUNT()));
  }

  @Override
  @JsonIgnore
  public ValidatorIndex getTargetCommitteeSize() {
    return new ValidatorIndex(UInt64.valueOf(getTARGET_COMMITTEE_SIZE()));
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxValidatorsPerCommittee() {
    return UInt64.valueOf(getMAX_VALIDATORS_PER_COMMITTEE());
  }

  @Override
  @JsonIgnore
  public UInt64 getMinPerEpochChurnLimit() {
    return UInt64.valueOf(getMIN_PER_EPOCH_CHURN_LIMIT());
  }

  @Override
  @JsonIgnore
  public UInt64 getChurnLimitQuotient() {
    return UInt64.valueOf(getCHURN_LIMIT_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public int getShuffleRoundCount() {
    return Integer.valueOf(getSHUFFLE_ROUND_COUNT());
  }

  @Override
  @JsonIgnore
  public UInt64 getMinGenesisActiveValidatorCount() {
    return UInt64.valueOf(getMIN_GENESIS_ACTIVE_VALIDATOR_COUNT());
  }

  @Override
  @JsonIgnore
  public Time getMinGenesisTime() {
    return Time.castFrom(UInt64.valueOf(getMIN_GENESIS_TIME()));
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSHARD_COUNT() {
    return SHARD_COUNT;
  }

  public void setSHARD_COUNT(String SHARD_COUNT) {
    this.SHARD_COUNT = SHARD_COUNT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getTARGET_COMMITTEE_SIZE() {
    return TARGET_COMMITTEE_SIZE;
  }

  public void setTARGET_COMMITTEE_SIZE(String TARGET_COMMITTEE_SIZE) {
    this.TARGET_COMMITTEE_SIZE = TARGET_COMMITTEE_SIZE;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_VALIDATORS_PER_COMMITTEE() {
    return MAX_VALIDATORS_PER_COMMITTEE;
  }

  public void setMAX_VALIDATORS_PER_COMMITTEE(String MAX_VALIDATORS_PER_COMMITTEE) {
    this.MAX_VALIDATORS_PER_COMMITTEE = MAX_VALIDATORS_PER_COMMITTEE;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_PER_EPOCH_CHURN_LIMIT() {
    return MIN_PER_EPOCH_CHURN_LIMIT;
  }

  public void setMIN_PER_EPOCH_CHURN_LIMIT(String MIN_PER_EPOCH_CHURN_LIMIT) {
    this.MIN_PER_EPOCH_CHURN_LIMIT = MIN_PER_EPOCH_CHURN_LIMIT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getCHURN_LIMIT_QUOTIENT() {
    return CHURN_LIMIT_QUOTIENT;
  }

  public void setCHURN_LIMIT_QUOTIENT(String CHURN_LIMIT_QUOTIENT) {
    this.CHURN_LIMIT_QUOTIENT = CHURN_LIMIT_QUOTIENT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSHUFFLE_ROUND_COUNT() {
    return SHUFFLE_ROUND_COUNT;
  }

  public void setSHUFFLE_ROUND_COUNT(int SHUFFLE_ROUND_COUNT) {
    this.SHUFFLE_ROUND_COUNT = String.valueOf(SHUFFLE_ROUND_COUNT);
  }

  public void setSHUFFLE_ROUND_COUNT(String SHUFFLE_ROUND_COUNT) {
    this.SHUFFLE_ROUND_COUNT = SHUFFLE_ROUND_COUNT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_GENESIS_ACTIVE_VALIDATOR_COUNT() {
    return MIN_GENESIS_ACTIVE_VALIDATOR_COUNT;
  }

  public void setMIN_GENESIS_ACTIVE_VALIDATOR_COUNT(String MIN_GENESIS_ACTIVE_VALIDATOR_COUNT) {
    this.MIN_GENESIS_ACTIVE_VALIDATOR_COUNT = MIN_GENESIS_ACTIVE_VALIDATOR_COUNT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_GENESIS_TIME() {
    return MIN_GENESIS_TIME;
  }

  public void setMIN_GENESIS_TIME(String MIN_GENESIS_TIME) {
    this.MIN_GENESIS_TIME = MIN_GENESIS_TIME;
  }
}
