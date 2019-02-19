package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.MiscParameters;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt64;

public class MiscParametersData implements MiscParameters {

  @JsonProperty(value = "SHARD_COUNT", access = JsonProperty.Access.WRITE_ONLY)
  private String SHARD_COUNT;
  @JsonProperty(value = "TARGET_COMMITTEE_SIZE", access = JsonProperty.Access.WRITE_ONLY)
  private String TARGET_COMMITTEE_SIZE;
  @JsonProperty(value = "EJECTION_BALANCE", access = JsonProperty.Access.WRITE_ONLY)
  private String EJECTION_BALANCE;
  @JsonProperty(value = "MAX_BALANCE_CHURN_QUOTIENT", access = JsonProperty.Access.WRITE_ONLY)
  private String MAX_BALANCE_CHURN_QUOTIENT;
  @JsonProperty(value = "BEACON_CHAIN_SHARD_NUMBER", access = JsonProperty.Access.WRITE_ONLY)
  private String BEACON_CHAIN_SHARD_NUMBER;
  @JsonProperty(value = "MAX_INDICES_PER_SLASHABLE_VOTE", access = JsonProperty.Access.WRITE_ONLY)
  private String MAX_INDICES_PER_SLASHABLE_VOTE;
  @JsonProperty(value = "MAX_WITHDRAWALS_PER_EPOCH", access = JsonProperty.Access.WRITE_ONLY)
  private String MAX_WITHDRAWALS_PER_EPOCH;

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
  public Gwei getEjectionBalance() {
    return Gwei.castFrom(UInt64.valueOf(getEJECTION_BALANCE()));
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxBalanceChurnQuotient() {
    return UInt64.valueOf(getMAX_BALANCE_CHURN_QUOTIENT());
  }

  @Override
  @JsonIgnore
  public ShardNumber getBeaconChainShardNumber() {
    return ShardNumber.of(UInt64.valueOf(getBEACON_CHAIN_SHARD_NUMBER()));
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxIndicesPerSlashableVote() {
    return UInt64.valueOf(getMAX_INDICES_PER_SLASHABLE_VOTE());
  }

  @Override
  @JsonIgnore
  public UInt64 getMaxWithdrawalsPerEpoch() {
    return UInt64.valueOf(getMAX_WITHDRAWALS_PER_EPOCH());
  }

  public String getSHARD_COUNT() {
    return SHARD_COUNT;
  }

  public void setSHARD_COUNT(String SHARD_COUNT) {
    this.SHARD_COUNT = SHARD_COUNT;
  }

  public String getTARGET_COMMITTEE_SIZE() {
    return TARGET_COMMITTEE_SIZE;
  }

  public void setTARGET_COMMITTEE_SIZE(String TARGET_COMMITTEE_SIZE) {
    this.TARGET_COMMITTEE_SIZE = TARGET_COMMITTEE_SIZE;
  }

  public String getEJECTION_BALANCE() {
    return EJECTION_BALANCE;
  }

  public void setEJECTION_BALANCE(String EJECTION_BALANCE) {
    this.EJECTION_BALANCE = EJECTION_BALANCE;
  }

  public String getMAX_BALANCE_CHURN_QUOTIENT() {
    return MAX_BALANCE_CHURN_QUOTIENT;
  }

  public void setMAX_BALANCE_CHURN_QUOTIENT(String MAX_BALANCE_CHURN_QUOTIENT) {
    this.MAX_BALANCE_CHURN_QUOTIENT = MAX_BALANCE_CHURN_QUOTIENT;
  }

  public String getBEACON_CHAIN_SHARD_NUMBER() {
    return BEACON_CHAIN_SHARD_NUMBER;
  }

  public void setBEACON_CHAIN_SHARD_NUMBER(String BEACON_CHAIN_SHARD_NUMBER) {
    this.BEACON_CHAIN_SHARD_NUMBER = BEACON_CHAIN_SHARD_NUMBER;
  }

  public String getMAX_INDICES_PER_SLASHABLE_VOTE() {
    return MAX_INDICES_PER_SLASHABLE_VOTE;
  }

  public void setMAX_INDICES_PER_SLASHABLE_VOTE(String MAX_INDICES_PER_SLASHABLE_VOTE) {
    this.MAX_INDICES_PER_SLASHABLE_VOTE = MAX_INDICES_PER_SLASHABLE_VOTE;
  }

  public String getMAX_WITHDRAWALS_PER_EPOCH() {
    return MAX_WITHDRAWALS_PER_EPOCH;
  }

  public void setMAX_WITHDRAWALS_PER_EPOCH(String MAX_WITHDRAWALS_PER_EPOCH) {
    this.MAX_WITHDRAWALS_PER_EPOCH = MAX_WITHDRAWALS_PER_EPOCH;
  }
}
