package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BlockData {
  private String slot;

  @JsonProperty("parent_root")
  private String parentRoot;

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

    private List<AttestationData> attestations;
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

    public List<AttestationData> getAttestations() {
      return attestations;
    }

    public void setAttestations(List<AttestationData> attestations) {
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
      private BlockHeaderData header1;

      @JsonProperty("header_2")
      private BlockHeaderData header2;

      public Long getProposerIndex() {
        return proposerIndex;
      }

      public void setProposerIndex(Long proposerIndex) {
        this.proposerIndex = proposerIndex;
      }

      public BlockHeaderData getHeader1() {
        return header1;
      }

      public void setHeader1(BlockHeaderData header1) {
        this.header1 = header1;
      }

      public BlockHeaderData getHeader2() {
        return header2;
      }

      public void setHeader2(BlockHeaderData header2) {
        this.header2 = header2;
      }
    }

    public static class IndexedAttestationData {
      @JsonProperty("custody_bit_0_indices")
      private List<Long> custodyBit0Indices;

      @JsonProperty("custody_bit_1_indices")
      private List<Long> custodyBit1Indices;

      @JsonProperty("data")
      private AttestationData.AttestationDataContainer data;

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

      public AttestationData.AttestationDataContainer getData() {
        return data;
      }

      public void setData(AttestationData.AttestationDataContainer data) {
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

      public void setSlashableAttestation1(IndexedAttestationData slashableAttestation1) {
        this.slashableAttestation1 = slashableAttestation1;
      }

      public IndexedAttestationData getSlashableAttestation2() {
        return slashableAttestation2;
      }

      public void setSlashableAttestation2(IndexedAttestationData slashableAttestation2) {
        this.slashableAttestation2 = slashableAttestation2;
      }
    }

    public static class DepositData {
      private List<String> proof;
      private DepositDataContainer data;

      public List<String> getProof() {
        return proof;
      }

      public void setProof(List<String> proof) {
        this.proof = proof;
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

      private CrossLinkData crosslink;

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

      public String getTargetEpoch() {
        return targetEpoch;
      }

      public void setTargetEpoch(String targetEpoch) {
        this.targetEpoch = targetEpoch;
      }

      public CrossLinkData getCrosslink() {
        return crosslink;
      }

      public void setCrosslink(CrossLinkData crosslink) {
        this.crosslink = crosslink;
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
