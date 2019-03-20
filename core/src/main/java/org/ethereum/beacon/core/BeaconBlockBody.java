package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.InitialValues;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#beaconblockbody">BeaconBlockBody
 *     in the spec</a>
 */
@SSZSerializable
public class BeaconBlockBody {

  /** A body where all lists are empty. */
  public static final BeaconBlockBody EMPTY =
      new BeaconBlockBody(
          InitialValues.EMPTY_SIGNATURE,
          Eth1Data.EMPTY,
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList());

  /** RANDAO signature submitted by proposer. */
  @SSZ private final BLSSignature randaoReveal;
  /** Eth1 data that is observed by proposer. */
  @SSZ private final Eth1Data eth1Data;
  /** A list of proposer slashing challenges. */
  @SSZ private final List<ProposerSlashing> proposerSlashingsList;
  /** A list of attester slashing challenges. */
  @SSZ private final List<AttesterSlashing> attesterSlashingsList;
  /** A list of attestations. */
  @SSZ private final List<Attestation> attestationsList;
  /** A list of validator deposit proofs. */
  @SSZ private final List<Deposit> depositsList;
  /** A list of validator exits. */
  @SSZ private final List<VoluntaryExit> voluntaryExitsList;
  /** A list of transfers. */
  @SSZ private final List<Transfer> transferList;

  public BeaconBlockBody(
      BLSSignature randaoReveal,
      Eth1Data eth1Data,
      List<ProposerSlashing> proposerSlashingsList,
      List<AttesterSlashing> attesterSlashingsList,
      List<Attestation> attestationsList,
      List<Deposit> depositsList,
      List<VoluntaryExit> voluntaryExitsList,
      List<Transfer> transferList) {
    this.randaoReveal = randaoReveal;
    this.eth1Data = eth1Data;
    this.proposerSlashingsList = proposerSlashingsList;
    this.attesterSlashingsList = attesterSlashingsList;
    this.attestationsList = attestationsList;
    this.depositsList = depositsList;
    this.voluntaryExitsList = voluntaryExitsList;
    this.transferList = transferList;
  }

  public BLSSignature getRandaoReveal() {
    return randaoReveal;
  }

  public Eth1Data getEth1Data() {
    return eth1Data;
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

  public ReadList<Integer, VoluntaryExit> getVoluntaryExits() {
    return WriteList.wrap(voluntaryExitsList, Integer::intValue);
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
  public List<VoluntaryExit> getVoluntaryExitsList() {
    return voluntaryExitsList;
  }

  /**
   * @deprecated for serialization only
   */
  public List<Transfer> getTransferList() {
    return transferList;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    BeaconBlockBody blockBody = (BeaconBlockBody) object;
    return Objects.equal(randaoReveal, blockBody.randaoReveal)
        && Objects.equal(eth1Data, blockBody.eth1Data)
        && Objects.equal(proposerSlashingsList, blockBody.proposerSlashingsList)
        && Objects.equal(attesterSlashingsList, blockBody.attesterSlashingsList)
        && Objects.equal(attestationsList, blockBody.attestationsList)
        && Objects.equal(depositsList, blockBody.depositsList)
        && Objects.equal(voluntaryExitsList, blockBody.voluntaryExitsList)
        && Objects.equal(transferList, blockBody.transferList);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        randaoReveal,
        eth1Data,
        proposerSlashingsList,
        attesterSlashingsList,
        attestationsList,
        depositsList,
        voluntaryExitsList,
        transferList);
  }
}
