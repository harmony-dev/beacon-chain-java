package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.BlsSignedTestCase;
import org.ethereum.beacon.test.type.NamedTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.AttestationData.AttestationDataContainer;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.StateTestUtils.parseAttestationData;
import static org.ethereum.beacon.test.StateTestUtils.parseBeaconBlockHeader;
import static org.ethereum.beacon.test.StateTestUtils.parseBlockData;
import static org.ethereum.beacon.test.StateTestUtils.parseSlashableAttestation;
import static org.ethereum.beacon.test.StateTestUtils.parseTransfer;
import static org.ethereum.beacon.test.StateTestUtils.parseVoluntaryExit;

/**
 * State test case <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/state">https://github.com/ethereum/eth2.0-tests/tree/master/state</a>
 */
public class StateTestCase implements NamedTestCase, BlsSignedTestCase {
  private String description;
  private BeaconStateData pre;
  private BeaconStateData post;
  @JsonProperty("bls_setting")
  private Integer blsSetting;

  @JsonProperty
  private BlockData.BlockBodyData.DepositData deposit;
  @JsonProperty
  private BeaconStateData.AttestationData attestation;
  @JsonProperty("attester_slashing")
  private BlockData.BlockBodyData.AttesterSlashingData attesterSlashing;
  @JsonProperty("proposer_slashing")
  private BlockData.BlockBodyData.ProposerSlashingData proposerSlashing;
  @JsonProperty
  private BlockData.BlockBodyData.TransferData transfer;
  @JsonProperty("voluntary_exit")
  private BlockData.BlockBodyData.VoluntaryExitData voluntaryExit;
  @JsonProperty
  private BlockData block;
  @JsonProperty
  private List<BlockData> blocks;
  @JsonProperty
  private Integer slots;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BlockData.BlockBodyData.DepositData getDeposit() {
    return deposit;
  }

  public Deposit getDepositOperation() {
    Deposit deposit =
        Deposit.create(
            getDeposit().getProof().stream().map(Hash32::fromHexString).collect(Collectors.toList()),
            new DepositData(
                BLSPubkey.fromHexString(getDeposit().getData().getPubkey()),
                Hash32.fromHexString(getDeposit().getData().getWithdrawalCredentials()),
                Gwei.castFrom(UInt64.valueOf(getDeposit().getData().getAmount())),
                BLSSignature.wrap(
                    Bytes96.fromHexString(getDeposit().getData().getSignature()))));

    return deposit;
  }

  public void setDeposit(BlockData.BlockBodyData.DepositData deposit) {
    this.deposit = deposit;
  }

  public BeaconStateData.AttestationData getAttestation() {
    return attestation;
  }

  public Attestation getAttestationOperation() {
    Attestation attestation =
        new Attestation(
            Bitfield.of(BytesValue.fromHexString(getAttestation().getAggregationBitfield())),
            parseAttestationData((getAttestation().getData())),
            Bitfield.of(BytesValue.fromHexString(getAttestation().getCustodyBitfield())),
            BLSSignature.wrap(Bytes96.fromHexString(getAttestation().getSignature())));

    return attestation;
  }

  public AttesterSlashing getAttesterSlashingOperation() {
    return new AttesterSlashing(
        parseSlashableAttestation(getAttesterSlashing().slashableAttestation1),
        parseSlashableAttestation(getAttesterSlashing().slashableAttestation2));
  }

  public ProposerSlashing getProposerSlashingOperation() {
    return new ProposerSlashing(
        ValidatorIndex.of(getProposerSlashing().proposerIndex),
        parseBeaconBlockHeader(getProposerSlashing().getHeader1()),
        parseBeaconBlockHeader(getProposerSlashing().getHeader2()));
  }

  public Transfer getTransferOperation() {
    return parseTransfer(getTransfer());
  }

  public VoluntaryExit getVoluntaryExitOperation() {
    return parseVoluntaryExit(getVoluntaryExit());
  }

  public BeaconBlock getBeaconBlock() {
    return parseBlockData(getBlock());
  }


  public void setAttestation(BeaconStateData.AttestationData attestation) {
    this.attestation = attestation;
  }

  public BlockData.BlockBodyData.AttesterSlashingData getAttesterSlashing() {
    return attesterSlashing;
  }

  public void setAttesterSlashing(BlockData.BlockBodyData.AttesterSlashingData attesterSlashing) {
    this.attesterSlashing = attesterSlashing;
  }

  public BlockData.BlockBodyData.ProposerSlashingData getProposerSlashing() {
    return proposerSlashing;
  }

  public void setProposerSlashing(BlockData.BlockBodyData.ProposerSlashingData proposerSlashing) {
    this.proposerSlashing = proposerSlashing;
  }

  public BlockData.BlockBodyData.TransferData getTransfer() {
    return transfer;
  }

  public void setTransfer(BlockData.BlockBodyData.TransferData transfer) {
    this.transfer = transfer;
  }

  public BlockData.BlockBodyData.VoluntaryExitData getVoluntaryExit() {
    return voluntaryExit;
  }

  public void setVoluntaryExit(BlockData.BlockBodyData.VoluntaryExitData voluntaryExit) {
    this.voluntaryExit = voluntaryExit;
  }

  public BlockData getBlock() {
    return block;
  }

  public void setBlock(BlockData block) {
    this.block = block;
  }

  public Integer getSlots() {
    return slots;
  }

  public void setSlots(Integer slots) {
    this.slots = slots;
  }

  public List<BlockData> getBlocks() {
    return blocks;
  }

  public List<BeaconBlock> getBeaconBlocks() {
    return blocks.stream().map(StateTestUtils::parseBlockData).collect(Collectors.toList());
  }

  public void setBlocks(List<BlockData> blocks) {
    this.blocks = blocks;
  }

  public BeaconStateData getPre() {
    return pre;
  }

  public void setPre(BeaconStateData pre) {
    this.pre = pre;
  }

  public BeaconStateData getPost() {
    return post;
  }

  public void setPost(BeaconStateData post) {
    this.post = post;
  }

  @Override
  public Integer getBlsSetting() {
    return blsSetting;
  }

  public void setBlsSetting(Integer blsSetting) {
    this.blsSetting = blsSetting;
  }

  @Override
  public String getName() {
    return getDescription();
  }

  @Override
  public String toString() {
    return "StateTestCase{" + "description='" + description + '\'' + '}';
  }

  public static class BeaconStateData {
    private String slot;

    @JsonProperty("genesis_time")
    private Long genesisTime;

    private Fork fork;

    @JsonProperty("validator_registry")
    private List<ValidatorData> validatorRegistry;

    private List<String> balances;

    @JsonProperty("latest_randao_mixes")
    private List<String> latestRandaoMixes;

    @JsonProperty("latest_start_shard")
    private Long latestStartShard;

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

    @JsonProperty("current_crosslinks")
    private List<CrossLinkData> currentCrosslinks;

    @JsonProperty("previous_crosslinks")
    private List<CrossLinkData> previousCrosslinks;

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
    private List<BlockData.BlockBodyData.Eth1> eth1DataVotes;

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

    public List<String> getBalances() {
      return balances;
    }

    public void setBalances(List<String> balances) {
      this.balances = balances;
    }

    public List<String> getLatestRandaoMixes() {
      return latestRandaoMixes;
    }

    public void setLatestRandaoMixes(List<String> latestRandaoMixes) {
      this.latestRandaoMixes = latestRandaoMixes;
    }

    public Long getLatestStartShard() {
      return latestStartShard;
    }

    public void setLatestStartShard(Long latestStartShard) {
      this.latestStartShard = latestStartShard;
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

    public List<BlockData.BlockBodyData.Eth1> getEth1DataVotes() {
      return eth1DataVotes;
    }

    public void setEth1DataVotes(List<BlockData.BlockBodyData.Eth1> eth1DataVotes) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttestationData {
      @JsonProperty("aggregation_bitfield")
      private String aggregationBitfield;

      private AttestationDataContainer data;

      @JsonProperty("custody_bitfield")
      private String custodyBitfield;

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
        @JsonProperty("beacon_block_root")
        private String beaconBlockRoot;

        @JsonProperty("source_epoch")
        private String sourceEpoch;

        @JsonProperty("source_root")
        private String sourceRoot;

        @JsonProperty("target_epoch")
        private String targetEpoch;

        @JsonProperty("target_root")
        private String targetRoot;

        private Long shard;

        @JsonProperty("previous_crosslink_root")
        private String previousCrosslinkRoot;

        @JsonProperty("crosslink_data_root")
        private String crosslinkDataRoot;

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

        public String getPreviousCrosslinkRoot() {
          return previousCrosslinkRoot;
        }

        public void setPreviousCrosslinkRoot(String previousCrosslinkRoot) {
          this.previousCrosslinkRoot = previousCrosslinkRoot;
        }

        public String getCrosslinkDataRoot() {
          return crosslinkDataRoot;
        }

        public void setCrosslinkDataRoot(String crosslinkDataRoot) {
          this.crosslinkDataRoot = crosslinkDataRoot;
        }

        public String getTargetEpoch() {
          return targetEpoch;
        }

        public void setTargetEpoch(String targetEpoch) {
          this.targetEpoch = targetEpoch;
        }
      }
    }

    public static class CrossLinkData {
      private String epoch;

      @JsonProperty("previous_crosslink_root")
      private String previousCrosslinkRoot;

      @JsonProperty("crosslink_data_root")
      private String crosslinkDataRoot;

      public String getEpoch() {
        return epoch;
      }

      public void setEpoch(String epoch) {
        this.epoch = epoch;
      }

      public String getPreviousCrosslinkRoot() {
        return previousCrosslinkRoot;
      }

      public void setPreviousCrosslinkRoot(String previousCrosslinkRoot) {
        this.previousCrosslinkRoot = previousCrosslinkRoot;
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

      private String graffiti;

      @JsonProperty("proposer_slashings")
      private List<ProposerSlashingData> proposerSlashings;

      @JsonProperty("attester_slashings")
      private List<AttesterSlashingData> attesterSlashings;

      private List<BeaconStateData.AttestationData> attestations;
      private List<DepositData> deposits;

      @JsonProperty("voluntary_exits")
      private List<VoluntaryExitData> voluntaryExits;

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

      public String getGraffiti() {
        return graffiti;
      }

      public void setGraffiti(String graffiti) {
        this.graffiti = graffiti;
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

      public List<VoluntaryExitData> getVoluntaryExits() {
        return voluntaryExits;
      }

      public void setVoluntaryExits(List<VoluntaryExitData> voluntaryExits) {
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

        @JsonProperty("deposit_count")
        private String depositCount;

        @JsonProperty("block_hash")
        private String blockHash;

        public String getDepositRoot() {
          return depositRoot;
        }

        public void setDepositRoot(String depositRoot) {
          this.depositRoot = depositRoot;
        }

        public String getDepositCount() {
          return depositCount;
        }

        public void setDepositCount(String depositCount) {
          this.depositCount = depositCount;
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

      public static class IndexedAttestationData {
        @JsonProperty("custody_bit_0_indices")
        private List<Long> custodyBit0Indices;
        @JsonProperty("custody_bit_1_indices")
        private List<Long> custodyBit1Indices;
        @JsonProperty("data")
        private AttestationDataContainer data;
        @JsonProperty("signature")
        private String signature;

        public String getSignature() {
          return signature;
        }

        public void setSignature(String signature) {
          this.signature = signature;
        }

        public List<Long> getCustodyBit0Indices() {
          return custodyBit0Indices;
        }

        public void setCustodyBit0Indices(List<Long> custodyBit0Indices) {
          this.custodyBit0Indices = custodyBit0Indices;
        }

        public List<Long> getCustodyBit1Indices() {
          return custodyBit1Indices;
        }

        public void setCustodyBit1Indices(List<Long> custodyBit1Indices) {
          this.custodyBit1Indices = custodyBit1Indices;
        }

        public AttestationDataContainer getData() {
          return data;
        }

        public void setData(AttestationDataContainer data) {
          this.data = data;
        }

        public String getAggregateSignature() {
          return signature;
        }

        public void setAggregateSignature(String aggregateSignature) {
          this.signature = aggregateSignature;
        }
      }

      public static class AttesterSlashingData {
        @JsonProperty("attestation_1")
        private IndexedAttestationData slashableAttestation1;
        @JsonProperty("attestation_2")
        private IndexedAttestationData slashableAttestation2;

        public IndexedAttestationData getSlashableAttestation1() {
          return slashableAttestation1;
        }

        public void setSlashableAttestation1(
            IndexedAttestationData slashableAttestation1) {
          this.slashableAttestation1 = slashableAttestation1;
        }

        public IndexedAttestationData getSlashableAttestation2() {
          return slashableAttestation2;
        }

        public void setSlashableAttestation2(
            IndexedAttestationData slashableAttestation2) {
          this.slashableAttestation2 = slashableAttestation2;
        }
      }

      public static class DepositData {
        private List<String> proof;
        private Long index;

        private DepositDataContainer data;

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

        public DepositDataContainer getData() {
          return data;
        }

        public void setData(DepositDataContainer data) {
          this.data = data;
        }

        public static class DepositDataContainer {
          private String pubkey;
          @JsonProperty("withdrawal_credentials")
          private String withdrawalCredentials;
          private String amount;
          private String signature;

          public String getAmount() {
            return amount;
          }

          public void setAmount(String amount) {
            this.amount = amount;
          }

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

          public String getSignature() {
            return signature;
          }

          public void setSignature(String signature) {
            this.signature = signature;
          }
        }
      }

      public static class VoluntaryExitData {
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
