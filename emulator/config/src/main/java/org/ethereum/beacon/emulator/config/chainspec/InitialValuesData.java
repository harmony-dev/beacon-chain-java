package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.InitialValues;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialValuesData implements InitialValues {

  @JsonProperty("GENESIS_FORK_VERSION")
  private String GENESIS_FORK_VERSION;
  @JsonProperty("GENESIS_SLOT")
  private String GENESIS_SLOT;
  @JsonProperty("GENESIS_START_SHARD")
  private Integer GENESIS_START_SHARD;
  @JsonProperty("FAR_FUTURE_EPOCH")
  private String FAR_FUTURE_EPOCH;
  @JsonProperty("ZERO_HASH")
  private String ZERO_HASH;
  @JsonProperty("EMPTY_SIGNATURE")
  private String EMPTY_SIGNATURE;
  @JsonProperty("BLS_WITHDRAWAL_PREFIX_BYTE")
  private String BLS_WITHDRAWAL_PREFIX_BYTE;

  @Override
  public UInt64 getGenesisForkVersion() {
    return UInt64.valueOf(getGENESIS_FORK_VERSION());
  }

  @Override
  public SlotNumber getGenesisSlot() {
    return SlotNumber.castFrom(UInt64.valueOf(getGENESIS_SLOT()));
  }

  @Override
  public EpochNumber getGenesisEpoch() {
    return null;// XXX: is set in final ChainSpec construction
  }

  @Override
  public ShardNumber getGenesisStartShard() {
    return ShardNumber.of(getGENESIS_START_SHARD());
  }

  @Override
  public EpochNumber getFarFutureEpoch() {
    return new EpochNumber(UInt64.valueOf(getFAR_FUTURE_EPOCH()));
  }

  @Override
  public Hash32 getZeroHash() {
    return Hash32.fromHexString(getZERO_HASH());
  }

  @Override
  public BLSSignature getEmptySignature() {
    return BLSSignature.wrap(Bytes96.fromHexString(getEMPTY_SIGNATURE()));
  }

  @Override
  public Bytes1 getBlsWithdrawalPrefixByte() {
    return Bytes1.fromHexString(getBLS_WITHDRAWAL_PREFIX_BYTE());
  }

  public String getGENESIS_FORK_VERSION() {
    return GENESIS_FORK_VERSION;
  }

  public void setGENESIS_FORK_VERSION(String GENESIS_FORK_VERSION) {
    this.GENESIS_FORK_VERSION = GENESIS_FORK_VERSION;
  }

  public String getGENESIS_SLOT() {
    return GENESIS_SLOT;
  }

  public void setGENESIS_SLOT(String GENESIS_SLOT) {
    this.GENESIS_SLOT = GENESIS_SLOT;
  }

  public Integer getGENESIS_START_SHARD() {
    return GENESIS_START_SHARD;
  }

  public void setGENESIS_START_SHARD(Integer GENESIS_START_SHARD) {
    this.GENESIS_START_SHARD = GENESIS_START_SHARD;
  }

  public String getFAR_FUTURE_EPOCH() {
    return FAR_FUTURE_EPOCH;
  }

  public void setFAR_FUTURE_EPOCH(String FAR_FUTURE_EPOCH) {
    this.FAR_FUTURE_EPOCH = FAR_FUTURE_EPOCH;
  }

  public String getZERO_HASH() {
    return ZERO_HASH;
  }

  public void setZERO_HASH(String ZERO_HASH) {
    this.ZERO_HASH = ZERO_HASH;
  }

  public String getEMPTY_SIGNATURE() {
    return EMPTY_SIGNATURE;
  }

  public void setEMPTY_SIGNATURE(String EMPTY_SIGNATURE) {
    this.EMPTY_SIGNATURE = EMPTY_SIGNATURE;
  }

  public String getBLS_WITHDRAWAL_PREFIX_BYTE() {
    return BLS_WITHDRAWAL_PREFIX_BYTE;
  }

  public void setBLS_WITHDRAWAL_PREFIX_BYTE(String BLS_WITHDRAWAL_PREFIX_BYTE) {
    this.BLS_WITHDRAWAL_PREFIX_BYTE = BLS_WITHDRAWAL_PREFIX_BYTE;
  }
}
