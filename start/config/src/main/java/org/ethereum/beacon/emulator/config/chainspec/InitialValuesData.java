package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.InitialValues;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialValuesData implements InitialValues {

  @JsonProperty("GENESIS_SLOT")
  private String GENESIS_SLOT;
  @JsonProperty("GENESIS_EPOCH")
  private String GENESIS_EPOCH;
  @JsonProperty("BLS_WITHDRAWAL_PREFIX")
  private String BLS_WITHDRAWAL_PREFIX;

  @Override
  @JsonIgnore
  public SlotNumber getGenesisSlot() {
    return SlotNumber.castFrom(UInt64.valueOf(getGENESIS_SLOT()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getGenesisEpoch() {
    return EpochNumber.castFrom(UInt64.valueOf(getGENESIS_EPOCH()));
  }

  @Override
  @JsonIgnore
  public UInt64 getBlsWithdrawalPrefix() {
    return UInt64.valueOf(getBLS_WITHDRAWAL_PREFIX());
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getGENESIS_SLOT() {
    return GENESIS_SLOT;
  }

  public void setGENESIS_SLOT(String GENESIS_SLOT) {
    this.GENESIS_SLOT = GENESIS_SLOT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getGENESIS_EPOCH() {
    return GENESIS_EPOCH;
  }

  public void setGENESIS_EPOCH(String GENESIS_EPOCH) {
    this.GENESIS_EPOCH = GENESIS_EPOCH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getBLS_WITHDRAWAL_PREFIX() {
    return BLS_WITHDRAWAL_PREFIX;
  }

  public void setBLS_WITHDRAWAL_PREFIX(String BLS_WITHDRAWAL_PREFIX) {
    this.BLS_WITHDRAWAL_PREFIX = BLS_WITHDRAWAL_PREFIX;
  }
}
