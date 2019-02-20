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

  @JsonProperty("SLOT_DURATION")
  private String SLOT_DURATION;
  @JsonProperty("MIN_ATTESTATION_INCLUSION_DELAY")
  private String MIN_ATTESTATION_INCLUSION_DELAY;
  @JsonProperty("EPOCH_LENGTH")
  private String EPOCH_LENGTH;
  @JsonProperty("SEED_LOOKAHEAD")
  private String  SEED_LOOKAHEAD;
  @JsonProperty("ENTRY_EXIT_DELAY")
  private String ENTRY_EXIT_DELAY;
  @JsonProperty("ETH1_DATA_VOTING_PERIOD")
  private String ETH1_DATA_VOTING_PERIOD;
  @JsonProperty("MIN_VALIDATOR_WITHDRAWAL_EPOCHS")
  private String MIN_VALIDATOR_WITHDRAWAL_EPOCHS;

  @Override
  @JsonIgnore
  public Time getSlotDuration() {
    return Time.castFrom(UInt64.valueOf(getSLOT_DURATION()));
  }

  @Override
  @JsonIgnore
  public SlotNumber getMinAttestationInclusionDelay() {
    return SlotNumber.castFrom(UInt64.valueOf(getMIN_ATTESTATION_INCLUSION_DELAY()));
  }

  @Override
  @JsonIgnore
  public EpochLength getEpochLength() {
    return new EpochLength(UInt64.valueOf(getEPOCH_LENGTH()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getSeedLookahead() {
    return new EpochNumber(UInt64.valueOf(getSEED_LOOKAHEAD()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getEntryExitDelay() {
    return new EpochNumber(UInt64.valueOf(getENTRY_EXIT_DELAY()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getEth1DataVotingPeriod() {
    return new EpochNumber(UInt64.valueOf(getETH1_DATA_VOTING_PERIOD()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getMinValidatorWithdrawalEpochs() {
    return new EpochNumber(UInt64.valueOf(getMIN_VALIDATOR_WITHDRAWAL_EPOCHS()));
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSLOT_DURATION() {
    return SLOT_DURATION;
  }

  public void setSLOT_DURATION(String SLOT_DURATION) {
    this.SLOT_DURATION = SLOT_DURATION;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_ATTESTATION_INCLUSION_DELAY() {
    return MIN_ATTESTATION_INCLUSION_DELAY;
  }

  public void setMIN_ATTESTATION_INCLUSION_DELAY(String MIN_ATTESTATION_INCLUSION_DELAY) {
    this.MIN_ATTESTATION_INCLUSION_DELAY = MIN_ATTESTATION_INCLUSION_DELAY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEPOCH_LENGTH() {
    return EPOCH_LENGTH;
  }

  public void setEPOCH_LENGTH(String EPOCH_LENGTH) {
    this.EPOCH_LENGTH = EPOCH_LENGTH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getSEED_LOOKAHEAD() {
    return SEED_LOOKAHEAD;
  }

  public void setSEED_LOOKAHEAD(String SEED_LOOKAHEAD) {
    this.SEED_LOOKAHEAD = SEED_LOOKAHEAD;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getENTRY_EXIT_DELAY() {
    return ENTRY_EXIT_DELAY;
  }

  public void setENTRY_EXIT_DELAY(String ENTRY_EXIT_DELAY) {
    this.ENTRY_EXIT_DELAY = ENTRY_EXIT_DELAY;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getETH1_DATA_VOTING_PERIOD() {
    return ETH1_DATA_VOTING_PERIOD;
  }

  public void setETH1_DATA_VOTING_PERIOD(String ETH1_DATA_VOTING_PERIOD) {
    this.ETH1_DATA_VOTING_PERIOD = ETH1_DATA_VOTING_PERIOD;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getMIN_VALIDATOR_WITHDRAWAL_EPOCHS() {
    return MIN_VALIDATOR_WITHDRAWAL_EPOCHS;
  }

  public void setMIN_VALIDATOR_WITHDRAWAL_EPOCHS(String MIN_VALIDATOR_WITHDRAWAL_EPOCHS) {
    this.MIN_VALIDATOR_WITHDRAWAL_EPOCHS = MIN_VALIDATOR_WITHDRAWAL_EPOCHS;
  }
}
