package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.StateListLengths;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.util.uint.UInt64;

public class StateListLengthsData implements StateListLengths {

  @JsonProperty("LATEST_BLOCK_ROOTS_LENGTH")
  private String LATEST_BLOCK_ROOTS_LENGTH;
  @JsonProperty("LATEST_RANDAO_MIXES_LENGTH")
  private String LATEST_RANDAO_MIXES_LENGTH;
  @JsonProperty("LATEST_ACTIVE_INDEX_ROOTS_LENGTH")
  private String LATEST_ACTIVE_INDEX_ROOTS_LENGTH;
  @JsonProperty("LATEST_SLASHED_EXIT_LENGTH")
  private String LATEST_SLASHED_EXIT_LENGTH;

  @Override
  @JsonIgnore
  public SlotNumber getLatestBlockRootsLength() {
    return SlotNumber.castFrom(UInt64.valueOf(getLATEST_BLOCK_ROOTS_LENGTH()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getLatestRandaoMixesLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_RANDAO_MIXES_LENGTH()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getLatestActiveIndexRootsLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_ACTIVE_INDEX_ROOTS_LENGTH()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getSlashedExitLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_SLASHED_EXIT_LENGTH()));
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getLATEST_BLOCK_ROOTS_LENGTH() {
    return LATEST_BLOCK_ROOTS_LENGTH;
  }

  public void setLATEST_BLOCK_ROOTS_LENGTH(String LATEST_BLOCK_ROOTS_LENGTH) {
    this.LATEST_BLOCK_ROOTS_LENGTH = LATEST_BLOCK_ROOTS_LENGTH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getLATEST_RANDAO_MIXES_LENGTH() {
    return LATEST_RANDAO_MIXES_LENGTH;
  }

  public void setLATEST_RANDAO_MIXES_LENGTH(String LATEST_RANDAO_MIXES_LENGTH) {
    this.LATEST_RANDAO_MIXES_LENGTH = LATEST_RANDAO_MIXES_LENGTH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getLATEST_ACTIVE_INDEX_ROOTS_LENGTH() {
    return LATEST_ACTIVE_INDEX_ROOTS_LENGTH;
  }

  public void setLATEST_ACTIVE_INDEX_ROOTS_LENGTH(String LATEST_ACTIVE_INDEX_ROOTS_LENGTH) {
    this.LATEST_ACTIVE_INDEX_ROOTS_LENGTH = LATEST_ACTIVE_INDEX_ROOTS_LENGTH;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getLATEST_SLASHED_EXIT_LENGTH() {
    return LATEST_SLASHED_EXIT_LENGTH;
  }

  public void setLATEST_SLASHED_EXIT_LENGTH(String LATEST_SLASHED_EXIT_LENGTH) {
    this.LATEST_SLASHED_EXIT_LENGTH = LATEST_SLASHED_EXIT_LENGTH;
  }
}
