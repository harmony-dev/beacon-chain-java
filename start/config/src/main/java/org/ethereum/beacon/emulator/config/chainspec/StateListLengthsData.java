package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.StateListLengths;
import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.util.uint.UInt64;

public class StateListLengthsData implements StateListLengths {

  @JsonProperty("EPOCHS_PER_HISTORICAL_VECTOR")
  private String EPOCHS_PER_HISTORICAL_VECTOR;
  @JsonProperty("EPOCHS_PER_SLASHINGS_VECTOR")
  private String EPOCHS_PER_SLASHINGS_VECTOR;
  @JsonProperty("HISTORICAL_ROOTS_LIMIT")
  private String HISTORICAL_ROOTS_LIMIT;
  @JsonProperty("VALIDATOR_REGISTRY_LIMIT")
  private String VALIDATOR_REGISTRY_LIMIT;

  @Override
  @JsonIgnore
  public EpochNumber getEpochsPerHistoricalVector() {
    return new EpochNumber(UInt64.valueOf(getEPOCHS_PER_HISTORICAL_VECTOR()));
  }

  @Override
  @JsonIgnore
  public EpochNumber getEpochsPerSlashingsVector() {
    return new EpochNumber(UInt64.valueOf(getEPOCHS_PER_SLASHINGS_VECTOR()));
  }

  @Override
  public UInt64 getHistoricalRootsLimit() {
    return UInt64.valueOf(getHISTORICAL_ROOTS_LIMIT());
  }

  @Override
  public UInt64 getValidatorRegistryLimit() {
    return UInt64.valueOf(getVALIDATOR_REGISTRY_LIMIT());
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEPOCHS_PER_HISTORICAL_VECTOR() {
    return EPOCHS_PER_HISTORICAL_VECTOR;
  }

  public void setEPOCHS_PER_HISTORICAL_VECTOR(String EPOCHS_PER_HISTORICAL_VECTOR) {
    this.EPOCHS_PER_HISTORICAL_VECTOR = EPOCHS_PER_HISTORICAL_VECTOR;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getEPOCHS_PER_SLASHINGS_VECTOR() {
    return EPOCHS_PER_SLASHINGS_VECTOR;
  }

  public void setEPOCHS_PER_SLASHINGS_VECTOR(String EPOCHS_PER_SLASHINGS_VECTOR) {
    this.EPOCHS_PER_SLASHINGS_VECTOR = EPOCHS_PER_SLASHINGS_VECTOR;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getHISTORICAL_ROOTS_LIMIT() {
    return HISTORICAL_ROOTS_LIMIT;
  }

  public void setHISTORICAL_ROOTS_LIMIT(String HISTORICAL_ROOTS_LIMIT) {
    this.HISTORICAL_ROOTS_LIMIT = HISTORICAL_ROOTS_LIMIT;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getVALIDATOR_REGISTRY_LIMIT() {
    return VALIDATOR_REGISTRY_LIMIT;
  }

  public void setVALIDATOR_REGISTRY_LIMIT(String VALIDATOR_REGISTRY_LIMIT) {
    this.VALIDATOR_REGISTRY_LIMIT = VALIDATOR_REGISTRY_LIMIT;
  }
}
