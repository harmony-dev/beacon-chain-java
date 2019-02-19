package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.MaxOperationsPerBlock;

public class MaxOperationsPerBlockData implements MaxOperationsPerBlock {

  @JsonProperty("MAX_PROPOSER_SLASHINGS")
  private Integer MAX_PROPOSER_SLASHINGS;
  @JsonProperty("MAX_ATTESTER_SLASHINGS")
  private Integer MAX_ATTESTER_SLASHINGS;
  @JsonProperty("MAX_ATTESTATIONS")
  private Integer MAX_ATTESTATIONS;
  @JsonProperty("MAX_DEPOSITS")
  private Integer MAX_DEPOSITS;
  @JsonProperty("MAX_EXITS")
  private Integer MAX_EXITS;

  @Override
  public int getMaxProposerSlashings() {
    return getMAX_PROPOSER_SLASHINGS();
  }

  @Override
  public int getMaxAttesterSlashings() {
    return getMAX_ATTESTER_SLASHINGS();
  }

  @Override
  public int getMaxAttestations() {
    return getMAX_ATTESTATIONS();
  }

  @Override
  public int getMaxDeposits() {
    return getMAX_DEPOSITS();
  }

  @Override
  public int getMaxExits() {
    return getMAX_EXITS();
  }

  public Integer getMAX_PROPOSER_SLASHINGS() {
    return MAX_PROPOSER_SLASHINGS;
  }

  public void setMAX_PROPOSER_SLASHINGS(Integer MAX_PROPOSER_SLASHINGS) {
    this.MAX_PROPOSER_SLASHINGS = MAX_PROPOSER_SLASHINGS;
  }

  public Integer getMAX_ATTESTER_SLASHINGS() {
    return MAX_ATTESTER_SLASHINGS;
  }

  public void setMAX_ATTESTER_SLASHINGS(Integer MAX_ATTESTER_SLASHINGS) {
    this.MAX_ATTESTER_SLASHINGS = MAX_ATTESTER_SLASHINGS;
  }

  public Integer getMAX_ATTESTATIONS() {
    return MAX_ATTESTATIONS;
  }

  public void setMAX_ATTESTATIONS(Integer MAX_ATTESTATIONS) {
    this.MAX_ATTESTATIONS = MAX_ATTESTATIONS;
  }

  public Integer getMAX_DEPOSITS() {
    return MAX_DEPOSITS;
  }

  public void setMAX_DEPOSITS(Integer MAX_DEPOSITS) {
    this.MAX_DEPOSITS = MAX_DEPOSITS;
  }

  public Integer getMAX_EXITS() {
    return MAX_EXITS;
  }

  public void setMAX_EXITS(Integer MAX_EXITS) {
    this.MAX_EXITS = MAX_EXITS;
  }
}
