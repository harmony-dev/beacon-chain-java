package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.List;

public class ValidatorDutiesResponse {
  List<ValidatorDuty> valdatorDutyList;

  @JsonCreator
  public ValidatorDutiesResponse(List<ValidatorDuty> valdatorDutyList) {
    this.valdatorDutyList = valdatorDutyList;
  }

  public List<ValidatorDuty> getValdatorDutyList() {
    return valdatorDutyList;
  }

  public static class ValidatorDuty {
    @JsonProperty("validator_pubkey")
    private String validatorPubkey;
    @JsonProperty("attestation_slot")
    private BigInteger attestationSlot;
    @JsonProperty("attestation_shard")
    private Integer attestationShard;
    @JsonProperty("block_proposal_slot")
    private BigInteger blockProposalSlot;

    public ValidatorDuty() {
    }

    public String getValidatorPubkey() {
      return validatorPubkey;
    }

    public void setValidatorPubkey(String validatorPubkey) {
      this.validatorPubkey = validatorPubkey;
    }

    public BigInteger getAttestationSlot() {
      return attestationSlot;
    }

    public void setAttestationSlot(BigInteger attestationSlot) {
      this.attestationSlot = attestationSlot;
    }

    public Integer getAttestationShard() {
      return attestationShard;
    }

    public void setAttestationShard(Integer attestationShard) {
      this.attestationShard = attestationShard;
    }

    public BigInteger getBlockProposalSlot() {
      return blockProposalSlot;
    }

    public void setBlockProposalSlot(BigInteger blockProposalSlot) {
      this.blockProposalSlot = blockProposalSlot;
    }
  }
}
