package org.ethereum.beacon.emulator.config.chainspec;

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
  @JsonProperty("LATEST_INDEX_ROOTS_LENGTH")
  private String LATEST_INDEX_ROOTS_LENGTH;
  @JsonProperty("LATEST_PENALIZED_EXIT_LENGTH")
  private String LATEST_PENALIZED_EXIT_LENGTH;

  @Override
  public SlotNumber getLatestBlockRootsLength() {
    return SlotNumber.castFrom(UInt64.valueOf(getLATEST_BLOCK_ROOTS_LENGTH()));
  }

  @Override
  public EpochNumber getLatestRandaoMixesLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_RANDAO_MIXES_LENGTH()));
  }

  @Override
  public EpochNumber getLatestIndexRootsLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_INDEX_ROOTS_LENGTH()));
  }

  @Override
  public EpochNumber getLatestPenalizedExitLength() {
    return new EpochNumber(UInt64.valueOf(getLATEST_PENALIZED_EXIT_LENGTH()));
  }

  public String getLATEST_BLOCK_ROOTS_LENGTH() {
    return LATEST_BLOCK_ROOTS_LENGTH;
  }

  public void setLATEST_BLOCK_ROOTS_LENGTH(String LATEST_BLOCK_ROOTS_LENGTH) {
    this.LATEST_BLOCK_ROOTS_LENGTH = LATEST_BLOCK_ROOTS_LENGTH;
  }

  public String getLATEST_RANDAO_MIXES_LENGTH() {
    return LATEST_RANDAO_MIXES_LENGTH;
  }

  public void setLATEST_RANDAO_MIXES_LENGTH(String LATEST_RANDAO_MIXES_LENGTH) {
    this.LATEST_RANDAO_MIXES_LENGTH = LATEST_RANDAO_MIXES_LENGTH;
  }

  public String getLATEST_INDEX_ROOTS_LENGTH() {
    return LATEST_INDEX_ROOTS_LENGTH;
  }

  public void setLATEST_INDEX_ROOTS_LENGTH(String LATEST_INDEX_ROOTS_LENGTH) {
    this.LATEST_INDEX_ROOTS_LENGTH = LATEST_INDEX_ROOTS_LENGTH;
  }

  public String getLATEST_PENALIZED_EXIT_LENGTH() {
    return LATEST_PENALIZED_EXIT_LENGTH;
  }

  public void setLATEST_PENALIZED_EXIT_LENGTH(String LATEST_PENALIZED_EXIT_LENGTH) {
    this.LATEST_PENALIZED_EXIT_LENGTH = LATEST_PENALIZED_EXIT_LENGTH;
  }
}