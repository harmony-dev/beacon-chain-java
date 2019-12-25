package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;

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

  public BeaconBlock createBlock(SpecConstants constants) {
    return BeaconBlockConverter.deserialize(this, constants);
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
      @JsonProperty("attesting_indices")
      private List<Long> attestingIndices;

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

      public List<Long> getAttestingIndices() {
        return attestingIndices;
      }

      public void setAttestingIndices(List<Long> attestingIndices) {
        this.attestingIndices = attestingIndices;
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
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AttestationData {
    @JsonProperty("aggregation_bits")
    private String aggregationBits;

    private AttestationDataContainer data;

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
}
