package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.BlsSignedTestCase;
import org.ethereum.beacon.test.type.NamedTestCase;
import org.ethereum.beacon.validator.api.model.BlockData;

import java.util.List;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.StateTestUtils.parseBlockData;
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

  @JsonProperty private BlockData.BlockBodyData.DepositData deposit;
  @JsonProperty private BlockData.AttestationData attestation;

  @JsonProperty("attester_slashing")
  private BlockData.BlockBodyData.AttesterSlashingData attesterSlashing;

  @JsonProperty("proposer_slashing")
  private BlockData.BlockBodyData.ProposerSlashingData proposerSlashing;

  @JsonProperty private BlockData.BlockBodyData.TransferData transfer;

  @JsonProperty("voluntary_exit")
  private BlockData.BlockBodyData.VoluntaryExitData voluntaryExit;

  @JsonProperty private BlockData block;
  @JsonProperty private List<BlockData> blocks;
  @JsonProperty private Integer slots;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BlockData.BlockBodyData.DepositData getDeposit() {
    return deposit;
  }

  public void setDeposit(BlockData.BlockBodyData.DepositData deposit) {
    this.deposit = deposit;
  }

  public Deposit getDepositOperation() {
    return StateTestUtils.parseDeposit(getDeposit());
  }

  public BlockData.AttestationData getAttestation() {
    return attestation;
  }

  public void setAttestation(BlockData.AttestationData attestation) {
    this.attestation = attestation;
  }

  public Attestation getAttestationOperation() {
    return StateTestUtils.parseAttestation(getAttestation());
  }

  public AttesterSlashing getAttesterSlashingOperation() {
    return StateTestUtils.parseAttesterSlashing(getAttesterSlashing());
  }

  public ProposerSlashing getProposerSlashingOperation() {
    return StateTestUtils.parseProposerSlashing(getProposerSlashing());
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

  public void setBlocks(List<BlockData> blocks) {
    this.blocks = blocks;
  }

  public List<BeaconBlock> getBeaconBlocks() {
    return blocks.stream().map(StateTestUtils::parseBlockData).collect(Collectors.toList());
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
    private List<BlockData.AttestationData> previousEpochAttestations;

    @JsonProperty("current_epoch_attestations")
    private List<BlockData.AttestationData> currentEpochAttestations;

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
    private List<BlockData.CrossLinkData> currentCrosslinks;

    @JsonProperty("previous_crosslinks")
    private List<BlockData.CrossLinkData> previousCrosslinks;

    @JsonProperty("latest_block_roots")
    private List<String> latestBlockRoots;

    @JsonProperty("latest_state_roots")
    private List<String> latestStateRoots;

    @JsonProperty("latest_active_index_roots")
    private List<String> latestActiveIndexRoots;

    @JsonProperty("latest_slashed_balances")
    private List<String> latestSlashedBalances;

    @JsonProperty("latest_block_header")
    private BlockData.BlockHeaderData latestBlockHeader;

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

    public List<BlockData.AttestationData> getPreviousEpochAttestations() {
      return previousEpochAttestations;
    }

    public void setPreviousEpochAttestations(
        List<BlockData.AttestationData> previousEpochAttestations) {
      this.previousEpochAttestations = previousEpochAttestations;
    }

    public List<BlockData.AttestationData> getCurrentEpochAttestations() {
      return currentEpochAttestations;
    }

    public void setCurrentEpochAttestations(
        List<BlockData.AttestationData> currentEpochAttestations) {
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

    public List<BlockData.CrossLinkData> getCurrentCrosslinks() {
      return currentCrosslinks;
    }

    public void setCurrentCrosslinks(List<BlockData.CrossLinkData> currentCrosslinks) {
      this.currentCrosslinks = currentCrosslinks;
    }

    public List<BlockData.CrossLinkData> getPreviousCrosslinks() {
      return previousCrosslinks;
    }

    public void setPreviousCrosslinks(List<BlockData.CrossLinkData> previousCrosslinks) {
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

    public BlockData.BlockHeaderData getLatestBlockHeader() {
      return latestBlockHeader;
    }

    public void setLatestBlockHeader(BlockData.BlockHeaderData latestBlockHeader) {
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
  }
}
