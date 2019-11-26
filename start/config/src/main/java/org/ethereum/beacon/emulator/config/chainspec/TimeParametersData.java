package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.TimeParameters;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.SlotNumber.EpochLength;
import org.ethereum.beacon.core.types.Time;
import tech.pegasys.artemis.util.uint.UInt64;

public class TimeParametersData implements TimeParameters {

  @JsonProperty("SECONDS_PER_SLOT")
  private String SECONDS_PER_SLOT;
  @JsonProperty("MIN_ATTESTATION_INCLUSION_DELAY")
  private String MIN_ATTESTATION_INCLUSION_DELAY;
  @JsonProperty("SLOTS_PER_EPOCH")
  private String SLOTS_PER_EPOCH;
  @JsonProperty("MIN_SEED_LOOKAHEAD")
  private String MIN_SEED_LOOKAHEAD;
  @JsonProperty("ACTIVATION_EXIT_DELAY")
  private String ACTIVATION_EXIT_DELAY;
  @JsonProperty("MAX_SEED_LOOKAHEAD")
  private String MAX_SEED_LOOKAHEAD;
  @JsonProperty("SLOTS_PER_ETH1_VOTING_PERIOD")
  private String SLOTS_PER_ETH1_VOTING_PERIOD;
  @JsonProperty("SLOTS_PER_HISTORICAL_ROOT")
  private String SLOTS_PER_HISTORICAL_ROOT;
  @JsonProperty("MIN_VALIDATOR_WITHDRAWABILITY_DELAY")
  private String MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
  @JsonProperty("PERSISTENT_COMMITTEE_PERIOD")
  private String PERSISTENT_COMMITTEE_PERIOD;
  @JsonProperty("MAX_EPOCHS_PER_CROSSLINK")
  private String MAX_EPOCHS_PER_CROSSLINK;
  @JsonProperty("MIN_EPOCHS_TO_INACTIVITY_PENALTY")
  private String MIN_EPOCHS_TO_INACTIVITY_PENALTY;

  @Override
  @JsonIgnore
  public Time getSecondsPerSlot() {
    return Time.castFrom(UInt64.valueOf(getSECONDS_PER_SLOT()));
  }

  @Override
  @JsonIgnore
  public SlotNumber getMinAttestationInclusionDelay() {
    return SlotNumber.castFrom(UInt64.valueOf(getMIN_ATTESTATION_INCLUSION_DELAY()));
  }

  @Override
  @JsonIgnore
  public EpochLength getSlotsPerEpoch() {
    return new EpochLength(UInt64.valueOf(getSLOTS_PER_EPOCH()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMinSeedLookahead() {
    return new EpochNumber(UInt64.valueOf(getMIN_SEED_LOOKAHEAD()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getActivationExitDelay() {
    return new EpochNumber(UInt64.valueOf(getACTIVATION_EXIT_DELAY()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMaxSeedLookahead() {
    return new EpochNumber(UInt64.valueOf(getMAX_SEED_LOOKAHEAD()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getSlotsPerEth1VotingPeriod() {
    return new EpochNumber(UInt64.valueOf(getSLOTS_PER_ETH1_VOTING_PERIOD()));
  }

  @Override
  @JsonIgnore
  public SlotNumber getSlotsPerHistoricalRoot() {
    return new SlotNumber(UInt64.valueOf(getSLOTS_PER_HISTORICAL_ROOT()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMinValidatorWithdrawabilityDelay() {
    return new EpochNumber(UInt64.valueOf(getMIN_VALIDATOR_WITHDRAWABILITY_DELAY()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getPersistentCommitteePeriod() {
    return new EpochNumber(UInt64.valueOf(getPERSISTENT_COMMITTEE_PERIOD()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMaxEpochsPerCrosslink() {
    return new EpochNumber(UInt64.valueOf(getMAX_EPOCHS_PER_CROSSLINK()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMinEpochsToInactivityPenalty() {
    return new EpochNumber(UInt64.valueOf(getMIN_EPOCHS_TO_INACTIVITY_PENALTY()));
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSECONDS_PER_SLOT() {
    return SECONDS_PER_SLOT;
  }

  public void setSECONDS_PER_SLOT(String SECONDS_PER_SLOT) {
    this.SECONDS_PER_SLOT = SECONDS_PER_SLOT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_ATTESTATION_INCLUSION_DELAY() {
    return MIN_ATTESTATION_INCLUSION_DELAY;
  }

  public void setMIN_ATTESTATION_INCLUSION_DELAY(String MIN_ATTESTATION_INCLUSION_DELAY) {
    this.MIN_ATTESTATION_INCLUSION_DELAY = MIN_ATTESTATION_INCLUSION_DELAY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSLOTS_PER_EPOCH() {
    return SLOTS_PER_EPOCH;
  }

  public void setSLOTS_PER_EPOCH(String SLOTS_PER_EPOCH) {
    this.SLOTS_PER_EPOCH = SLOTS_PER_EPOCH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_SEED_LOOKAHEAD() {
    return MIN_SEED_LOOKAHEAD;
  }

  public void setMIN_SEED_LOOKAHEAD(String MIN_SEED_LOOKAHEAD) {
    this.MIN_SEED_LOOKAHEAD = MIN_SEED_LOOKAHEAD;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getACTIVATION_EXIT_DELAY() {
    return ACTIVATION_EXIT_DELAY;
  }

  public void setACTIVATION_EXIT_DELAY(String ACTIVATION_EXIT_DELAY) {
    this.ACTIVATION_EXIT_DELAY = ACTIVATION_EXIT_DELAY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSLOTS_PER_ETH1_VOTING_PERIOD() {
    return SLOTS_PER_ETH1_VOTING_PERIOD;
  }

  public void setSLOTS_PER_ETH1_VOTING_PERIOD(String SLOTS_PER_ETH1_VOTING_PERIOD) {
    this.SLOTS_PER_ETH1_VOTING_PERIOD = SLOTS_PER_ETH1_VOTING_PERIOD;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSLOTS_PER_HISTORICAL_ROOT() {
    return SLOTS_PER_HISTORICAL_ROOT;
  }

  public void setSLOTS_PER_HISTORICAL_ROOT(String SLOTS_PER_HISTORICAL_ROOT) {
    this.SLOTS_PER_HISTORICAL_ROOT = SLOTS_PER_HISTORICAL_ROOT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_VALIDATOR_WITHDRAWABILITY_DELAY() {
    return MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
  }

  public void setMIN_VALIDATOR_WITHDRAWABILITY_DELAY(String MIN_VALIDATOR_WITHDRAWABILITY_DELAY) {
    this.MIN_VALIDATOR_WITHDRAWABILITY_DELAY = MIN_VALIDATOR_WITHDRAWABILITY_DELAY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getPERSISTENT_COMMITTEE_PERIOD() {
    return PERSISTENT_COMMITTEE_PERIOD;
  }

  public void setPERSISTENT_COMMITTEE_PERIOD(String PERSISTENT_COMMITTEE_PERIOD) {
    this.PERSISTENT_COMMITTEE_PERIOD = PERSISTENT_COMMITTEE_PERIOD;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_EPOCHS_PER_CROSSLINK() {
    return MAX_EPOCHS_PER_CROSSLINK;
  }

  public void setMAX_EPOCHS_PER_CROSSLINK(String MAX_EPOCHS_PER_CROSSLINK) {
    this.MAX_EPOCHS_PER_CROSSLINK = MAX_EPOCHS_PER_CROSSLINK;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_EPOCHS_TO_INACTIVITY_PENALTY() {
    return MIN_EPOCHS_TO_INACTIVITY_PENALTY;
  }

  public void setMIN_EPOCHS_TO_INACTIVITY_PENALTY(String MIN_EPOCHS_TO_INACTIVITY_PENALTY) {
    this.MIN_EPOCHS_TO_INACTIVITY_PENALTY = MIN_EPOCHS_TO_INACTIVITY_PENALTY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMAX_SEED_LOOKAHEAD() {
    return MAX_SEED_LOOKAHEAD;
  }

  public void setMAX_SEED_LOOKAHEAD(String MAX_SEED_LOOKAHEAD) {
    this.MAX_SEED_LOOKAHEAD = MAX_SEED_LOOKAHEAD;
  }
}
