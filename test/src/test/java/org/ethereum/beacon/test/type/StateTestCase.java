package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData.AttestationData;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData.AttestationData.AttestationDataContainer;

/**
 * State test case <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/state">https://github.com/ethereum/eth2.0-tests/tree/master/state</a>
 */
public class StateTestCase implements TestCase {
  private String name;
  private SpecConstantsDataMerged config;

  @JsonProperty("verify_signatures")
  private Boolean verifySignatures;

  @JsonProperty("initial_state")
  private BeaconStateData initialState;

  private List<BlockData> blocks;

  @JsonProperty("expected_state")
  private BeaconStateData expectedState;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SpecConstantsDataMerged getConfig() {
    return config;
  }

  public void setConfig(SpecConstantsDataMerged config) {
    this.config = config;
  }

  public Boolean getVerifySignatures() {
    return verifySignatures;
  }

  public void setVerifySignatures(Boolean verifySignatures) {
    this.verifySignatures = verifySignatures;
  }

  public BeaconStateData getInitialState() {
    return initialState;
  }

  public void setInitialState(BeaconStateData initialState) {
    this.initialState = initialState;
  }

  public List<BlockData> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<BlockData> blocks) {
    this.blocks = blocks;
  }

  public BeaconStateData getExpectedState() {
    return expectedState;
  }

  public void setExpectedState(BeaconStateData expectedState) {
    this.expectedState = expectedState;
  }

  @Override
  public String toString() {
    return "StateTestCase{" + "name='" + name + '\'' + '}';
  }

  public static class BeaconStateData {
    private String slot;

    @JsonProperty("genesis_time")
    private Long genesisTime;

    private Fork fork;

    @JsonProperty("validator_registry")
    private List<ValidatorData> validatorRegistry;

    @JsonProperty("validator_balances")
    private List<String> validatorBalances;

    @JsonProperty("validator_registry_update_epoch")
    private String validatorRegistryUpdateEpoch;

    @JsonProperty("latest_randao_mixes")
    private List<String> latestRandaoMixes;

    @JsonProperty("previous_shuffling_start_shard")
    private Long previousShufflingStartShard;

    @JsonProperty("current_shuffling_start_shard")
    private Long currentShufflingStartShard;

    @JsonProperty("previous_shuffling_epoch")
    private String previousShufflingEpoch;

    @JsonProperty("current_shuffling_epoch")
    private String currentShufflingEpoch;

    @JsonProperty("previous_shuffling_seed")
    private String previousShufflingSeed;

    @JsonProperty("current_shuffling_seed")
    private String currentShufflingSeed;

    @JsonProperty("previous_epoch_attestations")
    private List<AttestationData> previousEpochAttestations;

    @JsonProperty("current_epoch_attestations")
    private List<AttestationData> currentEpochAttestations;

    @JsonProperty("previous_justified_epoch")
    private String previousJustifiedEpoch;

    @JsonProperty("current_justified_epoch")
    private String currentJustifiedEpoch;

    @JsonProperty("previous_justified_root")
    private String previousJustifiedRoot;

    @JsonProperty("current_justified_root")
    private String currentJustifiedRoot;

    @JsonProperty("justification_bitfield")
    private String justificationBitfield;

    @JsonProperty("finalized_epoch")
    private String finalizedEpoch;

    @JsonProperty("finalized_root")
    private String finalizedRoot;

    @JsonProperty("latest_crosslinks")
    private List<CrossLinkData> latestCrosslinks;

    @JsonProperty("latest_block_roots")
    private List<String> latestBlockRoots;

    @JsonProperty("latest_state_roots")
    private List<String> latestStateRoots;

    @JsonProperty("latest_active_index_roots")
    private List<String> latestActiveIndexRoots;

    @JsonProperty("latest_slashed_balances")
    private List<String> latestSlashedBalances;

    @JsonProperty("latest_block_header")
    private BlockHeaderData latestBlockHeader;

    @JsonProperty("historical_roots")
    private List<String> historicalRoots;

    @JsonProperty("latest_eth1_data")
    private BlockData.BlockBodyData.Eth1 latestEth1Data;

    @JsonProperty("eth1_data_votes")
    private List<Eth1Vote> eth1DataVotes;

    @JsonProperty("deposit_index")
    private Long depositIndex;

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

    public List<ValidatorData> getValidatorRegistry() {
      return validatorRegistry;
    }

    public void setValidatorRegistry(List<ValidatorData> validatorRegistry) {
      this.validatorRegistry = validatorRegistry;
    }

    public List<String> getValidatorBalances() {
      return validatorBalances;
    }

    public void setValidatorBalances(List<String> validatorBalances) {
      this.validatorBalances = validatorBalances;
    }

    public String getValidatorRegistryUpdateEpoch() {
      return validatorRegistryUpdateEpoch;
    }

    public void setValidatorRegistryUpdateEpoch(String validatorRegistryUpdateEpoch) {
      this.validatorRegistryUpdateEpoch = validatorRegistryUpdateEpoch;
    }

    public List<String> getLatestRandaoMixes() {
      return latestRandaoMixes;
    }

    public void setLatestRandaoMixes(List<String> latestRandaoMixes) {
      this.latestRandaoMixes = latestRandaoMixes;
    }

    public Long getPreviousShufflingStartShard() {
      return previousShufflingStartShard;
    }

    public void setPreviousShufflingStartShard(Long previousShufflingStartShard) {
      this.previousShufflingStartShard = previousShufflingStartShard;
    }

    public Long getCurrentShufflingStartShard() {
      return currentShufflingStartShard;
    }

    public void setCurrentShufflingStartShard(Long currentShufflingStartShard) {
      this.currentShufflingStartShard = currentShufflingStartShard;
    }

    public String getPreviousShufflingEpoch() {
      return previousShufflingEpoch;
    }

    public void setPreviousShufflingEpoch(String previousShufflingEpoch) {
      this.previousShufflingEpoch = previousShufflingEpoch;
    }

    public String getCurrentShufflingEpoch() {
      return currentShufflingEpoch;
    }

    public void setCurrentShufflingEpoch(String currentShufflingEpoch) {
      this.currentShufflingEpoch = currentShufflingEpoch;
    }

    public String getPreviousShufflingSeed() {
      return previousShufflingSeed;
    }

    public void setPreviousShufflingSeed(String previousShufflingSeed) {
      this.previousShufflingSeed = previousShufflingSeed;
    }

    public String getCurrentShufflingSeed() {
      return currentShufflingSeed;
    }

    public void setCurrentShufflingSeed(String currentShufflingSeed) {
      this.currentShufflingSeed = currentShufflingSeed;
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

    public String getPreviousJustifiedEpoch() {
      return previousJustifiedEpoch;
    }

    public void setPreviousJustifiedEpoch(String previousJustifiedEpoch) {
      this.previousJustifiedEpoch = previousJustifiedEpoch;
    }

    public String getCurrentJustifiedEpoch() {
      return currentJustifiedEpoch;
    }

    public void setCurrentJustifiedEpoch(String currentJustifiedEpoch) {
      this.currentJustifiedEpoch = currentJustifiedEpoch;
    }

    public String getPreviousJustifiedRoot() {
      return previousJustifiedRoot;
    }

    public void setPreviousJustifiedRoot(String previousJustifiedRoot) {
      this.previousJustifiedRoot = previousJustifiedRoot;
    }

    public String getCurrentJustifiedRoot() {
      return currentJustifiedRoot;
    }

    public void setCurrentJustifiedRoot(String currentJustifiedRoot) {
      this.currentJustifiedRoot = currentJustifiedRoot;
    }

    public String getJustificationBitfield() {
      return justificationBitfield;
    }

    public void setJustificationBitfield(String justificationBitfield) {
      this.justificationBitfield = justificationBitfield;
    }

    public String getFinalizedEpoch() {
      return finalizedEpoch;
    }

    public void setFinalizedEpoch(String finalizedEpoch) {
      this.finalizedEpoch = finalizedEpoch;
    }

    public String getFinalizedRoot() {
      return finalizedRoot;
    }

    public void setFinalizedRoot(String finalizedRoot) {
      this.finalizedRoot = finalizedRoot;
    }

    public List<CrossLinkData> getLatestCrosslinks() {
      return latestCrosslinks;
    }

    public void setLatestCrosslinks(List<CrossLinkData> latestCrosslinks) {
      this.latestCrosslinks = latestCrosslinks;
    }

    public List<String> getLatestBlockRoots() {
      return latestBlockRoots;
    }

    public void setLatestBlockRoots(List<String> latestBlockRoots) {
      this.latestBlockRoots = latestBlockRoots;
    }

    public List<String> getLatestStateRoots() {
      return latestStateRoots;
    }

    public void setLatestStateRoots(List<String> latestStateRoots) {
      this.latestStateRoots = latestStateRoots;
    }

    public List<String> getLatestActiveIndexRoots() {
      return latestActiveIndexRoots;
    }

    public void setLatestActiveIndexRoots(List<String> latestActiveIndexRoots) {
      this.latestActiveIndexRoots = latestActiveIndexRoots;
    }

    public List<String> getLatestSlashedBalances() {
      return latestSlashedBalances;
    }

    public void setLatestSlashedBalances(List<String> latestSlashedBalances) {
      this.latestSlashedBalances = latestSlashedBalances;
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

    public BlockData.BlockBodyData.Eth1 getLatestEth1Data() {
      return latestEth1Data;
    }

    public void setLatestEth1Data(BlockData.BlockBodyData.Eth1 latestEth1Data) {
      this.latestEth1Data = latestEth1Data;
    }

    public List<Eth1Vote> getEth1DataVotes() {
      return eth1DataVotes;
    }

    public void setEth1DataVotes(List<Eth1Vote> eth1DataVotes) {
      this.eth1DataVotes = eth1DataVotes;
    }

    public Long getDepositIndex() {
      return depositIndex;
    }

    public void setDepositIndex(Long depositIndex) {
      this.depositIndex = depositIndex;
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

      @JsonProperty("exit_epoch")
      private String exitEpoch;

      @JsonProperty("withdrawable_epoch")
      private String withdrawableEpoch;

      @JsonProperty("initiated_exit")
      private Boolean initiatedExit;

      private Boolean slashed;

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

      public Boolean getInitiatedExit() {
        return initiatedExit;
      }

      public void setInitiatedExit(Boolean initiatedExit) {
        this.initiatedExit = initiatedExit;
      }

      public Boolean getSlashed() {
        return slashed;
      }

      public void setSlashed(Boolean slashed) {
        this.slashed = slashed;
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttestationData {
      @JsonProperty("aggregation_bitfield")
      private String aggregationBitfield;

      private AttestationDataContainer data;

      @JsonProperty("custody_bitfield")
      private String custodyBitfield;

      @JsonProperty("aggregate_signature")
      private String aggregateSignature;

      @JsonProperty("inclusion_slot")
      private String inclusionSlot;

      public String getInclusionSlot() {
        return inclusionSlot;
      }

      public void setInclusionSlot(String inclusionSlot) {
        this.inclusionSlot = inclusionSlot;
      }

      public String getAggregationBitfield() {
        return aggregationBitfield;
      }

      public void setAggregationBitfield(String aggregationBitfield) {
        this.aggregationBitfield = aggregationBitfield;
      }

      public AttestationDataContainer getData() {
        return data;
      }

      public void setData(AttestationDataContainer data) {
        this.data = data;
      }

      public String getCustodyBitfield() {
        return custodyBitfield;
      }

      public void setCustodyBitfield(String custodyBitfield) {
        this.custodyBitfield = custodyBitfield;
      }

      public String getAggregateSignature() {
        return aggregateSignature;
      }

      public void setAggregateSignature(String aggregateSignature) {
        this.aggregateSignature = aggregateSignature;
      }

      public static class AttestationDataContainer {
        private String slot;

        @JsonProperty("beacon_block_root")
        private String beaconBlockRoot;

        @JsonProperty("source_epoch")
        private String sourceEpoch;

        @JsonProperty("source_root")
        private String sourceRoot;

        @JsonProperty("target_root")
        private String targetRoot;

        private Long shard;

        @JsonProperty("previous_crosslink")
        private CrossLinkData previousCrosslink;

        @JsonProperty("crosslink_data_root")
        private String crosslinkDataRoot;

        public String getSlot() {
          return slot;
        }

        public void setSlot(String slot) {
          this.slot = slot;
        }

        public String getBeaconBlockRoot() {
          return beaconBlockRoot;
        }

        public void setBeaconBlockRoot(String beaconBlockRoot) {
          this.beaconBlockRoot = beaconBlockRoot;
        }

        public String getSourceEpoch() {
          return sourceEpoch;
        }

        public void setSourceEpoch(String sourceEpoch) {
          this.sourceEpoch = sourceEpoch;
        }

        public String getSourceRoot() {
          return sourceRoot;
        }

        public void setSourceRoot(String sourceRoot) {
          this.sourceRoot = sourceRoot;
        }

        public String getTargetRoot() {
          return targetRoot;
        }

        public void setTargetRoot(String targetRoot) {
          this.targetRoot = targetRoot;
        }

        public Long getShard() {
          return shard;
        }

        public void setShard(Long shard) {
          this.shard = shard;
        }

        public CrossLinkData getPreviousCrosslink() {
          return previousCrosslink;
        }

        public void setPreviousCrosslink(CrossLinkData previousCrosslink) {
          this.previousCrosslink = previousCrosslink;
        }

        public String getCrosslinkDataRoot() {
          return crosslinkDataRoot;
        }

        public void setCrosslinkDataRoot(String crosslinkDataRoot) {
          this.crosslinkDataRoot = crosslinkDataRoot;
        }
      }
    }

    public static class CrossLinkData {
      private String epoch;

      @JsonProperty("crosslink_data_root")
      private String crosslinkDataRoot;

      public String getEpoch() {
        return epoch;
      }

      public void setEpoch(String epoch) {
        this.epoch = epoch;
      }

      public String getCrosslinkDataRoot() {
        return crosslinkDataRoot;
      }

      public void setCrosslinkDataRoot(String crosslinkDataRoot) {
        this.crosslinkDataRoot = crosslinkDataRoot;
      }
    }

    public static class BlockHeaderData {
      private String slot;

      @JsonProperty("previous_block_root")
      private String previousBlockRoot;

      @JsonProperty("state_root")
      private String stateRoot;

      @JsonProperty("block_body_root")
      private String blockBodyRoot;

      private String signature;

      public String getSlot() {
        return slot;
      }

      public void setSlot(String slot) {
        this.slot = slot;
      }

      public String getPreviousBlockRoot() {
        return previousBlockRoot;
      }

      public void setPreviousBlockRoot(String previousBlockRoot) {
        this.previousBlockRoot = previousBlockRoot;
      }

      public String getStateRoot() {
        return stateRoot;
      }

      public void setStateRoot(String stateRoot) {
        this.stateRoot = stateRoot;
      }

      public String getBlockBodyRoot() {
        return blockBodyRoot;
      }

      public void setBlockBodyRoot(String blockBodyRoot) {
        this.blockBodyRoot = blockBodyRoot;
      }

      public String getSignature() {
        return signature;
      }

      public void setSignature(String signature) {
        this.signature = signature;
      }
    }

    public static class Eth1Vote {}
  }

  public static class BlockData {
    private String slot;

    @JsonProperty("previous_block_root")
    private String previousBlockRoot;

    @JsonProperty("state_root")
    private String stateRoot;

    private BlockBodyData body;
    private String signature;

    public String getSlot() {
      return slot;
    }

    public void setSlot(String slot) {
      this.slot = slot;
    }

    public String getPreviousBlockRoot() {
      return previousBlockRoot;
    }

    public void setPreviousBlockRoot(String previousBlockRoot) {
      this.previousBlockRoot = previousBlockRoot;
    }

    public String getStateRoot() {
      return stateRoot;
    }

    public void setStateRoot(String stateRoot) {
      this.stateRoot = stateRoot;
    }

    public BlockBodyData getBody() {
      return body;
    }

    public void setBody(BlockBodyData body) {
      this.body = body;
    }

    public String getSignature() {
      return signature;
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }

    public static class BlockBodyData {
      @JsonProperty("randao_reveal")
      private String randaoReveal;

      @JsonProperty("eth1_data")
      private Eth1 eth1Data;

      @JsonProperty("proposer_slashings")
      private List<ProposerSlashingData> proposerSlashings;

      @JsonProperty("attester_slashings")
      private List<AttesterSlashingData> attesterSlashings;

      private List<BeaconStateData.AttestationData> attestations;
      private List<DepositData> deposits;

      @JsonProperty("voluntary_exits")
      private List<ExitData> voluntaryExits;

      private List<TransferData> transfers;

      public String getRandaoReveal() {
        return randaoReveal;
      }

      public void setRandaoReveal(String randaoReveal) {
        this.randaoReveal = randaoReveal;
      }

      public Eth1 getEth1Data() {
        return eth1Data;
      }

      public void setEth1Data(Eth1 eth1Data) {
        this.eth1Data = eth1Data;
      }

      public List<ProposerSlashingData> getProposerSlashings() {
        return proposerSlashings;
      }

      public void setProposerSlashings(List<ProposerSlashingData> proposerSlashings) {
        this.proposerSlashings = proposerSlashings;
      }

      public List<AttesterSlashingData> getAttesterSlashings() {
        return attesterSlashings;
      }

      public void setAttesterSlashings(List<AttesterSlashingData> attesterSlashings) {
        this.attesterSlashings = attesterSlashings;
      }

      public List<BeaconStateData.AttestationData> getAttestations() {
        return attestations;
      }

      public void setAttestations(List<BeaconStateData.AttestationData> attestations) {
        this.attestations = attestations;
      }

      public List<DepositData> getDeposits() {
        return deposits;
      }

      public void setDeposits(List<DepositData> deposits) {
        this.deposits = deposits;
      }

      public List<ExitData> getVoluntaryExits() {
        return voluntaryExits;
      }

      public void setVoluntaryExits(List<ExitData> voluntaryExits) {
        this.voluntaryExits = voluntaryExits;
      }

      public List<TransferData> getTransfers() {
        return transfers;
      }

      public void setTransfers(List<TransferData> transfers) {
        this.transfers = transfers;
      }

      public static class Eth1 {
        @JsonProperty("deposit_root")
        private String depositRoot;

        @JsonProperty("block_hash")
        private String blockHash;

        public String getDepositRoot() {
          return depositRoot;
        }

        public void setDepositRoot(String depositRoot) {
          this.depositRoot = depositRoot;
        }

        public String getBlockHash() {
          return blockHash;
        }

        public void setBlockHash(String blockHash) {
          this.blockHash = blockHash;
        }
      }

      public static class ProposerSlashingData {
        @JsonProperty("proposer_index")
        private Long proposerIndex;

        @JsonProperty("header_1")
        private BeaconStateData.BlockHeaderData header1;

        @JsonProperty("header_2")
        private BeaconStateData.BlockHeaderData header2;

        public Long getProposerIndex() {
          return proposerIndex;
        }

        public void setProposerIndex(Long proposerIndex) {
          this.proposerIndex = proposerIndex;
        }

        public BeaconStateData.BlockHeaderData getHeader1() {
          return header1;
        }

        public void setHeader1(BeaconStateData.BlockHeaderData header1) {
          this.header1 = header1;
        }

        public BeaconStateData.BlockHeaderData getHeader2() {
          return header2;
        }

        public void setHeader2(BeaconStateData.BlockHeaderData header2) {
          this.header2 = header2;
        }
      }

      public static class SlashableAttestationData {
        @JsonProperty("validator_indices")
        private List<Long> validatorIndices;
        @JsonProperty("data")
        private AttestationDataContainer data;
        @JsonProperty("custody_bitfield")
        private String custodyBitfield;
        @JsonProperty("aggregate_signature")
        private String aggregateSignature;

        public List<Long> getValidatorIndices() {
          return validatorIndices;
        }

        public void setValidatorIndices(List<Long> validatorIndices) {
          this.validatorIndices = validatorIndices;
        }

        public AttestationDataContainer getData() {
          return data;
        }

        public void setData(AttestationDataContainer data) {
          this.data = data;
        }

        public String getCustodyBitfield() {
          return custodyBitfield;
        }

        public void setCustodyBitfield(String custodyBitfield) {
          this.custodyBitfield = custodyBitfield;
        }

        public String getAggregateSignature() {
          return aggregateSignature;
        }

        public void setAggregateSignature(String aggregateSignature) {
          this.aggregateSignature = aggregateSignature;
        }
      }

      public static class AttesterSlashingData {
        @JsonProperty("slashable_attestation_1")
        private SlashableAttestationData slashableAttestation1;
        @JsonProperty("slashable_attestation_2")
        private SlashableAttestationData slashableAttestation2;

        public SlashableAttestationData getSlashableAttestation1() {
          return slashableAttestation1;
        }

        public void setSlashableAttestation1(
            SlashableAttestationData slashableAttestation1) {
          this.slashableAttestation1 = slashableAttestation1;
        }

        public SlashableAttestationData getSlashableAttestation2() {
          return slashableAttestation2;
        }

        public void setSlashableAttestation2(
            SlashableAttestationData slashableAttestation2) {
          this.slashableAttestation2 = slashableAttestation2;
        }
      }

      public static class DepositData {
        private List<String> proof;
        private Long index;

        @JsonProperty("deposit_data")
        private DepositDataContainer depositData;

        public List<String> getProof() {
          return proof;
        }

        public void setProof(List<String> proof) {
          this.proof = proof;
        }

        public Long getIndex() {
          return index;
        }

        public void setIndex(Long index) {
          this.index = index;
        }

        public DepositDataContainer getDepositData() {
          return depositData;
        }

        public void setDepositData(DepositDataContainer depositData) {
          this.depositData = depositData;
        }

        public static class DepositDataContainer {
          private String amount;
          private Long timestamp;

          @JsonProperty("deposit_input")
          private DepositInputData depositInput;

          public String getAmount() {
            return amount;
          }

          public void setAmount(String amount) {
            this.amount = amount;
          }

          public Long getTimestamp() {
            return timestamp;
          }

          public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
          }

          public DepositInputData getDepositInput() {
            return depositInput;
          }

          public void setDepositInput(DepositInputData depositInput) {
            this.depositInput = depositInput;
          }

          public static class DepositInputData {
            private String pubkey;

            @JsonProperty("withdrawal_credentials")
            private String withdrawalCredentials;

            @JsonProperty("proof_of_possession")
            private String proofOfPossession;

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

            public String getProofOfPossession() {
              return proofOfPossession;
            }

            public void setProofOfPossession(String proofOfPossession) {
              this.proofOfPossession = proofOfPossession;
            }
          }
        }
      }

      public static class ExitData {
        private String epoch;

        @JsonProperty("validator_index")
        private Long validatorIndex;

        private String signature;

        public String getEpoch() {
          return epoch;
        }

        public void setEpoch(String epoch) {
          this.epoch = epoch;
        }

        public Long getValidatorIndex() {
          return validatorIndex;
        }

        public void setValidatorIndex(Long validatorIndex) {
          this.validatorIndex = validatorIndex;
        }

        public String getSignature() {
          return signature;
        }

        public void setSignature(String signature) {
          this.signature = signature;
        }
      }

      public static class TransferData {
        private Long sender;
        private Long recipient;
        private String amount;
        private String fee;
        private String slot;
        private String pubkey;
        private String signature;

        public Long getSender() {
          return sender;
        }

        public void setSender(Long sender) {
          this.sender = sender;
        }

        public Long getRecipient() {
          return recipient;
        }

        public void setRecipient(Long recipient) {
          this.recipient = recipient;
        }

        public String getAmount() {
          return amount;
        }

        public void setAmount(String amount) {
          this.amount = amount;
        }

        public String getFee() {
          return fee;
        }

        public void setFee(String fee) {
          this.fee = fee;
        }

        public String getSlot() {
          return slot;
        }

        public void setSlot(String slot) {
          this.slot = slot;
        }

        public String getPubkey() {
          return pubkey;
        }

        public void setPubkey(String pubkey) {
          this.pubkey = pubkey;
        }

        public String getSignature() {
          return signature;
        }

        public void setSignature(String signature) {
          this.signature = signature;
        }
      }

      @JsonIgnoreProperties(ignoreUnknown = false)
      public static class SomeData {}
    }
  }
}
