package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;

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
@SSZSerializable
public class BeaconBlockBody {

  /** A body where all lists are empty. */
  public static final BeaconBlockBody EMPTY =
      new BeaconBlockBody(
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList());

  /** A list of proposer slashing challenges. */
  @SSZ private final List<ProposerSlashing> proposerSlashingsList;

  @SSZ private final List<AttesterSlashing> attesterSlashingsList;
  /** A list of attestations. */
  @SSZ private final List<Attestation> attestationsList;
  /** A list of validator deposit proofs. */
  @SSZ private final List<Deposit> depositsList;
  /** A list of validator exits. */
  @SSZ private final List<VoluntaryExit> exitsList;
  /** A list of transfers. */
  @SSZ private final List<Transfer> transferList;

  public BeaconBlockBody(
      List<ProposerSlashing> proposerSlashings,
      List<AttesterSlashing> attesterSlashings,
      List<Attestation> attestations,
      List<Deposit> deposits,
      List<VoluntaryExit> voluntaryExits,
      List<Transfer> transfers) {
    this.proposerSlashingsList = new ArrayList<>(proposerSlashings);
    this.attesterSlashingsList = new ArrayList<>(attesterSlashings);
    this.attestationsList = new ArrayList<>(attestations);
    this.depositsList = new ArrayList<>(deposits);
    this.exitsList = new ArrayList<>(voluntaryExits);
    this.transferList = new ArrayList<>(transfers);
  }

  public ReadList<Integer, ProposerSlashing> getProposerSlashings() {
    return WriteList.wrap(proposerSlashingsList, Integer::intValue);
  }

  public ReadList<Integer, AttesterSlashing> getAttesterSlashings() {
    return WriteList.wrap(attesterSlashingsList, Integer::intValue);
  }

  public ReadList<Integer, Attestation> getAttestations() {
    return WriteList.wrap(attestationsList, Integer::intValue);
  }

  public ReadList<Integer, Deposit> getDeposits() {
    return WriteList.wrap(depositsList, Integer::intValue);
  }

  public ReadList<Integer, VoluntaryExit> getExits() {
    return WriteList.wrap(exitsList, Integer::intValue);
  }

  public ReadList<Integer, Transfer> getTransfers() {
    return WriteList.wrap(transferList, Integer::intValue);
  }

  /**
   * @deprecated for serialization only
   */
  public List<ProposerSlashing> getProposerSlashingsList() {
    return proposerSlashingsList;
  }

  /**
   * @deprecated for serialization only
   */
  public List<AttesterSlashing> getAttesterSlashingsList() {
    return attesterSlashingsList;
  }

  /**
   * @deprecated for serialization only
   */
  public List<Attestation> getAttestationsList() {
    return attestationsList;
  }

  /**
   * @deprecated for serialization only
   */
  public List<Deposit> getDepositsList() {
    return depositsList;
  }

  /**
   * @deprecated for serialization only
   */
  public List<VoluntaryExit> getExitsList() {
    return exitsList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconBlockBody that = (BeaconBlockBody) o;
    return Objects.equal(proposerSlashingsList, that.proposerSlashingsList)
        && Objects.equal(attestationsList, that.attestationsList)
        && Objects.equal(attesterSlashingsList, that.attesterSlashingsList)
        && Objects.equal(depositsList, that.depositsList)
        && Objects.equal(exitsList, that.exitsList);
  }
}
