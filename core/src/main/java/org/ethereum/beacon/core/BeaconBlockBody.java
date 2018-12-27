package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProofOfCustodyChallenge;
import org.ethereum.beacon.core.operations.ProofOfCustodyResponse;
import org.ethereum.beacon.core.operations.ProofOfCustodySeedChange;
import org.ethereum.beacon.core.operations.ProposerSlashing;

public class BeaconBlockBody {
  public static final BeaconBlockBody EMPTY =
      new BeaconBlockBody(
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList());

  private final List<ProposerSlashing> proposerSlashings;
  private final List<CasperSlashing> casperSlashings;
  private final List<Attestation> attestations;
  private final List<ProofOfCustodySeedChange> pocSeedChanges;
  private final List<ProofOfCustodyChallenge> pocChallenges;
  private final List<ProofOfCustodyResponse> pocResponses;
  private final List<Deposit> deposits;
  private final List<Exit> exits;

  public BeaconBlockBody(
      List<ProposerSlashing> proposerSlashings,
      List<CasperSlashing> casperSlashings,
      List<Attestation> attestations,
      List<ProofOfCustodySeedChange> pocSeedChanges,
      List<ProofOfCustodyChallenge> pocChallenges,
      List<ProofOfCustodyResponse> pocResponses,
      List<Deposit> deposits,
      List<Exit> exits) {
    this.proposerSlashings = proposerSlashings;
    this.casperSlashings = casperSlashings;
    this.attestations = attestations;
    this.pocSeedChanges = pocSeedChanges;
    this.pocChallenges = pocChallenges;
    this.pocResponses = pocResponses;
    this.deposits = deposits;
    this.exits = exits;
  }

  public List<ProposerSlashing> getProposerSlashings() {
    return proposerSlashings;
  }

  public List<CasperSlashing> getCasperSlashings() {
    return casperSlashings;
  }

  public List<Attestation> getAttestations() {
    return attestations;
  }

  public List<ProofOfCustodySeedChange> getPocSeedChanges() {
    return pocSeedChanges;
  }

  public List<ProofOfCustodyChallenge> getPocChallenges() {
    return pocChallenges;
  }

  public List<ProofOfCustodyResponse> getPocResponses() {
    return pocResponses;
  }

  public List<Deposit> getDeposits() {
    return deposits;
  }

  public List<Exit> getExits() {
    return exits;
  }
}
