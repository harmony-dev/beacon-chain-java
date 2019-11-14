package org.ethereum.beacon.test.type.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BeaconStateData {
  private String slot;

  @JsonProperty("genesis_time")
  private Long genesisTime;

  private Fork fork;

  private List<ValidatorData> validators;

  private List<String> balances;

  @JsonProperty("randao_mixes")
  private List<String> randaoMixes;

  @JsonProperty("start_shard")
  private Long startShard;

  @JsonProperty("previous_epoch_attestations")
  private List<AttestationData> previousEpochAttestations;

  @JsonProperty("current_epoch_attestations")
  private List<AttestationData> currentEpochAttestations;

  @JsonProperty("previous_justified_checkpoint")
  private CheckpointData previousJustifiedCheckpoint;

  @JsonProperty("current_justified_checkpoint")
  private CheckpointData currentJustifiedCheckpoint;

  @JsonProperty("justification_bits")
  private String justificationBits;

  @JsonProperty("finalized_checkpoint")
  private CheckpointData finalizedCheckpoint;

  @JsonProperty("current_crosslinks")
  private List<CrossLinkData> currentCrosslinks;

  @JsonProperty("previous_crosslinks")
  private List<CrossLinkData> previousCrosslinks;

  @JsonProperty("block_roots")
  private List<String> blockRoots;

  @JsonProperty("state_roots")
  private List<String> stateRoots;

  @JsonProperty("historical_roots")
  private List<String> historicalRoots;

  @JsonProperty("active_index_roots")
  private List<String> activeIndexRoots;

  @JsonProperty("compact_committees_roots")
  private List<String> compactCommitteesRoots;

  private List<String> slashings;

  @JsonProperty("latest_block_header")
  private BlockHeaderData latestBlockHeader;

  @JsonProperty("eth1_data")
  private BlockData.BlockBodyData.Eth1 eth1Data;

  @JsonProperty("eth1_data_votes")
  private List<BlockData.BlockBodyData.Eth1> eth1DataVotes;

  @JsonProperty("eth1_deposit_index")
  private Long eth1DepositIndex;

  public String getSlot() {
    return slot;
  }

  public void setSlot(String slot) {
    this.slot = slot;
  }

  public Long getGenesisTime() {
    return genesisTime;
  }

  public void setGenesisTime(Long genesisTime) {
    this.genesisTime = genesisTime;
  }

  public Fork getFork() {
    return fork;
  }

  public void setFork(Fork fork) {
    this.fork = fork;
  }

  public List<ValidatorData> getValidators() {
    return validators;
  }

  public void setValidators(List<ValidatorData> validators) {
    this.validators = validators;
  }

  public List<String> getBalances() {
    return balances;
  }

  public void setBalances(List<String> balances) {
    this.balances = balances;
  }

  public List<String> getRandaoMixes() {
    return randaoMixes;
  }

  public void setRandaoMixes(List<String> randaoMixes) {
    this.randaoMixes = randaoMixes;
  }

  public Long getStartShard() {
    return startShard;
  }

  public void setStartShard(Long startShard) {
    this.startShard = startShard;
  }

  public List<AttestationData> getPreviousEpochAttestations() {
    return previousEpochAttestations;
  }

  public void setPreviousEpochAttestations(List<AttestationData> previousEpochAttestations) {
    this.previousEpochAttestations = previousEpochAttestations;
  }

  public List<AttestationData> getCurrentEpochAttestations() {
    return currentEpochAttestations;
  }

  public void setCurrentEpochAttestations(List<AttestationData> currentEpochAttestations) {
    this.currentEpochAttestations = currentEpochAttestations;
  }

  public CheckpointData getPreviousJustifiedCheckpoint() {
    return previousJustifiedCheckpoint;
  }

  public void setPreviousJustifiedCheckpoint(CheckpointData previousJustifiedCheckpoint) {
    this.previousJustifiedCheckpoint = previousJustifiedCheckpoint;
  }

  public CheckpointData getCurrentJustifiedCheckpoint() {
    return currentJustifiedCheckpoint;
  }

  public void setCurrentJustifiedCheckpoint(CheckpointData currentJustifiedCheckpoint) {
    this.currentJustifiedCheckpoint = currentJustifiedCheckpoint;
  }

  public String getJustificationBits() {
    return justificationBits;
  }

  public void setJustificationBits(String justificationBits) {
    this.justificationBits = justificationBits;
  }

  public CheckpointData getFinalizedCheckpoint() {
    return finalizedCheckpoint;
  }

  public void setFinalizedCheckpoint(CheckpointData finalizedCheckpoint) {
    this.finalizedCheckpoint = finalizedCheckpoint;
  }

  public List<CrossLinkData> getCurrentCrosslinks() {
    return currentCrosslinks;
  }

  public void setCurrentCrosslinks(List<CrossLinkData> currentCrosslinks) {
    this.currentCrosslinks = currentCrosslinks;
  }

  public List<CrossLinkData> getPreviousCrosslinks() {
    return previousCrosslinks;
  }

  public void setPreviousCrosslinks(List<CrossLinkData> previousCrosslinks) {
    this.previousCrosslinks = previousCrosslinks;
  }

  public List<String> getBlockRoots() {
    return blockRoots;
  }

  public void setBlockRoots(List<String> blockRoots) {
    this.blockRoots = blockRoots;
  }

  public List<String> getStateRoots() {
    return stateRoots;
  }

  public void setStateRoots(List<String> stateRoots) {
    this.stateRoots = stateRoots;
  }

  public List<String> getActiveIndexRoots() {
    return activeIndexRoots;
  }

  public void setActiveIndexRoots(List<String> activeIndexRoots) {
    this.activeIndexRoots = activeIndexRoots;
  }

  public List<String> getCompactCommitteesRoots() {
    return compactCommitteesRoots;
  }

  public void setCompactCommitteesRoots(List<String> compactCommitteesRoots) {
    this.compactCommitteesRoots = compactCommitteesRoots;
  }

  public List<String> getSlashings() {
    return slashings;
  }

  public void setSlashings(List<String> slashings) {
    this.slashings = slashings;
  }

  public BlockHeaderData getLatestBlockHeader() {
    return latestBlockHeader;
  }

  public void setLatestBlockHeader(BlockHeaderData latestBlockHeader) {
    this.latestBlockHeader = latestBlockHeader;
  }

  public List<String> getHistoricalRoots() {
    return historicalRoots;
  }

  public void setHistoricalRoots(List<String> historicalRoots) {
    this.historicalRoots = historicalRoots;
  }

  public BlockData.BlockBodyData.Eth1 getEth1Data() {
    return eth1Data;
  }

  public void setEth1Data(BlockData.BlockBodyData.Eth1 eth1Data) {
    this.eth1Data = eth1Data;
  }

  public List<BlockData.BlockBodyData.Eth1> getEth1DataVotes() {
    return eth1DataVotes;
  }

  public void setEth1DataVotes(List<BlockData.BlockBodyData.Eth1> eth1DataVotes) {
    this.eth1DataVotes = eth1DataVotes;
  }

  public Long getEth1DepositIndex() {
    return eth1DepositIndex;
  }

  public void setEth1DepositIndex(Long eth1DepositIndex) {
    this.eth1DepositIndex = eth1DepositIndex;
  }

  public static class Fork {
    @JsonProperty("previous_version")
    private String previousVersion;

    @JsonProperty("current_version")
    private String currentVersion;

    private String epoch;

    public String getPreviousVersion() {
      return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
      this.previousVersion = previousVersion;
    }

    public String getCurrentVersion() {
      return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
      this.currentVersion = currentVersion;
    }

    public String getEpoch() {
      return epoch;
    }

    public void setEpoch(String epoch) {
      this.epoch = epoch;
    }
  }

  public static class ValidatorData {
    private String pubkey;

    @JsonProperty("withdrawal_credentials")
    private String withdrawalCredentials;

    @JsonProperty("activation_epoch")
    private String activationEpoch;

    @JsonProperty("activation_eligibility_epoch")
    private String activationEligibilityEpoch;

    @JsonProperty("exit_epoch")
    private String exitEpoch;

    @JsonProperty("withdrawable_epoch")
    private String withdrawableEpoch;

    private Boolean slashed;

    @JsonProperty("effective_balance")
    private String effectiveBalance;

    public String getPubkey() {
      return pubkey;
    }

    public void setPubkey(String pubkey) {
      this.pubkey = pubkey;
    }

    public String getWithdrawalCredentials() {
      return withdrawalCredentials;
    }

    public void setWithdrawalCredentials(String withdrawalCredentials) {
      this.withdrawalCredentials = withdrawalCredentials;
    }

    public String getActivationEpoch() {
      return activationEpoch;
    }

    public void setActivationEpoch(String activationEpoch) {
      this.activationEpoch = activationEpoch;
    }

    public String getActivationEligibilityEpoch() {
      return activationEligibilityEpoch;
    }

    public void setActivationEligibilityEpoch(String activationEligibilityEpoch) {
      this.activationEligibilityEpoch = activationEligibilityEpoch;
    }

    public String getExitEpoch() {
      return exitEpoch;
    }

    public void setExitEpoch(String exitEpoch) {
      this.exitEpoch = exitEpoch;
    }

    public String getWithdrawableEpoch() {
      return withdrawableEpoch;
    }

    public void setWithdrawableEpoch(String withdrawableEpoch) {
      this.withdrawableEpoch = withdrawableEpoch;
    }

    public Boolean getSlashed() {
      return slashed;
    }

    public void setSlashed(Boolean slashed) {
      this.slashed = slashed;
    }

    public String getEffectiveBalance() {
      return effectiveBalance;
    }

    public void setEffectiveBalance(String effectiveBalance) {
      this.effectiveBalance = effectiveBalance;
    }
  }

  public static class CheckpointData {
    private String root;
    private Long epoch;

    public String getRoot() {
      return root;
    }

    public void setRoot(String root) {
      this.root = root;
    }

    public Long getEpoch() {
      return epoch;
    }

    public void setEpoch(Long epoch) {
      this.epoch = epoch;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AttestationData {
    @JsonProperty("aggregation_bits")
    private String aggregationBits;

    private AttestationDataContainer data;

    @JsonProperty("custody_bits")
    private String custodyBits;

    private String signature;

    @JsonProperty("inclusion_delay")
    private String inclusionDelay;

    @JsonProperty("proposer_index")
    private Long proposerIndex;

    public String getInclusionDelay() {
      return inclusionDelay;
    }

    public void setInclusionDelay(String inclusionDelay) {
      this.inclusionDelay = inclusionDelay;
    }

    public String getAggregationBits() {
      return aggregationBits;
    }

    public void setAggregationBits(String aggregationBits) {
      this.aggregationBits = aggregationBits;
    }

    public AttestationDataContainer getData() {
      return data;
    }

    public void setData(AttestationDataContainer data) {
      this.data = data;
    }

    public String getCustodyBits() {
      return custodyBits;
    }

    public void setCustodyBits(String custodyBits) {
      this.custodyBits = custodyBits;
    }

    public String getSignature() {
      return signature;
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }

    public Long getProposerIndex() {
      return proposerIndex;
    }

    public void setProposerIndex(Long proposerIndex) {
      this.proposerIndex = proposerIndex;
    }

    public static class AttestationDataContainer {
      private Long slot;

      private Long index;

      @JsonProperty("beacon_block_root")
      private String beaconBlockRoot;

      private CheckpointData source;

      private CheckpointData target;

      public String getBeaconBlockRoot() {
        return beaconBlockRoot;
      }

      public void setBeaconBlockRoot(String beaconBlockRoot) {
        this.beaconBlockRoot = beaconBlockRoot;
      }

      public CheckpointData getSource() {
        return source;
      }

      public void setSource(CheckpointData source) {
        this.source = source;
      }

      public CheckpointData getTarget() {
        return target;
      }

      public void setTarget(CheckpointData target) {
        this.target = target;
      }

      public Long getSlot() {
        return slot;
      }

      public void setSlot(Long slot) {
        this.slot = slot;
      }

      public Long getIndex() {
        return index;
      }

      public void setIndex(Long index) {
        this.index = index;
      }
    }
  }

  public static class CrossLinkData {
    private Long shard;

    @JsonProperty("start_epoch")
    private String startEpoch;

    @JsonProperty("end_epoch")
    private String endEpoch;

    @JsonProperty("parent_root")
    private String parentRoot;

    @JsonProperty("data_root")
    private String dataRoot;

    public Long getShard() {
      return shard;
    }

    public void setShard(Long shard) {
      this.shard = shard;
    }

    public String getStartEpoch() {
      return startEpoch;
    }

    public void setStartEpoch(String startEpoch) {
      this.startEpoch = startEpoch;
    }

    public String getEndEpoch() {
      return endEpoch;
    }

    public void setEndEpoch(String endEpoch) {
      this.endEpoch = endEpoch;
    }

    public String getParentRoot() {
      return parentRoot;
    }

    public void setParentRoot(String parentRoot) {
      this.parentRoot = parentRoot;
    }

    public String getDataRoot() {
      return dataRoot;
    }

    public void setDataRoot(String dataRoot) {
      this.dataRoot = dataRoot;
    }
  }

  public static class BlockHeaderData {
    private String slot;

    @JsonProperty("parent_root")
    private String parentRoot;

    @JsonProperty("state_root")
    private String stateRoot;

    @JsonProperty("body_root")
    private String bodyRoot;

    private String signature;

    public String getSlot() {
      return slot;
    }

    public void setSlot(String slot) {
      this.slot = slot;
    }

    public String getParentRoot() {
      return parentRoot;
    }

    public void setParentRoot(String parentRoot) {
      this.parentRoot = parentRoot;
    }

    public String getStateRoot() {
      return stateRoot;
    }

    public void setStateRoot(String stateRoot) {
      this.stateRoot = stateRoot;
    }

    public String getBodyRoot() {
      return bodyRoot;
    }

    public void setBodyRoot(String bodyRoot) {
      this.bodyRoot = bodyRoot;
    }

    public String getSignature() {
      return signature;
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }
  }
}
