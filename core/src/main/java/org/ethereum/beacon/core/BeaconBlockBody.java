package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.operations.CustodyResponse;
import org.ethereum.beacon.core.operations.CustodyReseed;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
/**
 * Beacon block body.
 *
 * <p>Contains lists of beacon chain operations.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beaconblockbody">BeaconBlockBody
 *     in the spec</a>
 */
public class BeaconBlockBody {

  /** A body where all lists are empty. */
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

  /** A list of proposer slashing challenges. */
  private final List<ProposerSlashing> proposerSlashings;
  /** A list of Casper slashing challenges. */
  private final List<CasperSlashing> casperSlashings;
  /** A list of attestations. */
  private final List<Attestation> attestations;
  /** A list of proof of custody seed changes. */
  private final List<CustodyReseed> custodyReseeds;
  /** A list of proof of custody challenges. */
  private final List<CustodyChallenge> custodyChallenges;
  /** A list of proof of custody challenge responses. */
  private final List<CustodyResponse> custodyResponses;
  /** A list of validator deposit proofs. */
  private final List<Deposit> deposits;
  /** A list of validator exits. */
  private final List<Exit> exits;

  public BeaconBlockBody(
      List<ProposerSlashing> proposerSlashings,
      List<CasperSlashing> casperSlashings,
      List<Attestation> attestations,
      List<CustodyReseed> custodyReseeds,
      List<CustodyChallenge> custodyChallenges,
      List<CustodyResponse> custodyResponses,
      List<Deposit> deposits,
      List<Exit> exits) {
    this.proposerSlashings = proposerSlashings;
    this.casperSlashings = casperSlashings;
    this.attestations = attestations;
    this.custodyReseeds = custodyReseeds;
    this.custodyChallenges = custodyChallenges;
    this.custodyResponses = custodyResponses;
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

  public List<CustodyReseed> getCustodyReseeds() {
    return custodyReseeds;
  }

  public List<CustodyChallenge> getCustodyChallenges() {
    return custodyChallenges;
  }

  public List<CustodyResponse> getCustodyResponses() {
    return custodyResponses;
  }

  public List<Deposit> getDeposits() {
    return deposits;
  }

  public List<Exit> getExits() {
    return exits;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconBlockBody that = (BeaconBlockBody) o;
    return proposerSlashings.equals(that.proposerSlashings) &&
        casperSlashings.equals(that.casperSlashings) &&
        attestations.equals(that.attestations) &&
        custodyReseeds.equals(that.custodyReseeds) &&
        custodyChallenges.equals(that.custodyChallenges) &&
        custodyResponses.equals(that.custodyResponses) &&
        deposits.equals(that.deposits) &&
        exits.equals(that.exits);
  }
}
