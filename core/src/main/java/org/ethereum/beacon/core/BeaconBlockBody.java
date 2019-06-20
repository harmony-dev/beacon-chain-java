package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import com.google.common.base.Objects;
import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.collections.ReadList;

/**
 * Beacon block body.
 *
 * <p>Contains lists of beacon chain operations.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.0/specs/core/0_beacon-chain.md#beaconblockbody">BeaconBlockBody
 *     in the spec</a>
 */
@SSZSerializable
public class BeaconBlockBody {

  /** A body where all lists are empty. */
  public static final BeaconBlockBody EMPTY =
      BeaconBlockBody.create(
          BLSSignature.ZERO,
          Eth1Data.EMPTY,
          Bytes32.ZERO,
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
  /** Analogue to Eth1 Extra Data. */
  @SSZ private final Bytes32 graffiti;
  /** A list of proposer slashing challenges. */
  @SSZ private final ReadList<Integer, ProposerSlashing> proposerSlashings;
  /** A list of attester slashing challenges. */
  @SSZ private final ReadList<Integer, AttesterSlashing> attesterSlashings;
  /** A list of attestations. */
  @SSZ private final ReadList<Integer, Attestation> attestations;
  /** A list of validator deposit proofs. */
  @SSZ private final ReadList<Integer, Deposit> deposits;
  /** A list of validator exits. */
  @SSZ private final ReadList<Integer, VoluntaryExit> voluntaryExits;
  /** A list of transfers. */
  @SSZ private final ReadList<Integer, Transfer> transfers;

  public BeaconBlockBody(
      BLSSignature randaoReveal,
      Eth1Data eth1Data,
      Bytes32 graffiti,
      ReadList<Integer, ProposerSlashing> proposerSlashings,
      ReadList<Integer, AttesterSlashing> attesterSlashings,
      ReadList<Integer, Attestation> attestations,
      ReadList<Integer, Deposit> deposits,
      ReadList<Integer, VoluntaryExit> voluntaryExits,
      ReadList<Integer, Transfer> transfers) {
    this.randaoReveal = randaoReveal;
    this.eth1Data = eth1Data;
    this.graffiti = graffiti;
    this.proposerSlashings = proposerSlashings;
    this.attesterSlashings = attesterSlashings;
    this.attestations = attestations;
    this.deposits = deposits;
    this.voluntaryExits = voluntaryExits;
    this.transfers = transfers;
  }

  public static BeaconBlockBody create(
      BLSSignature randaoReveal,
      Eth1Data eth1Data,
      Bytes32 graffiti,
      List<ProposerSlashing> proposerSlashings,
      List<AttesterSlashing> attesterSlashings,
      List<Attestation> attestations,
      List<Deposit> deposits,
      List<VoluntaryExit> voluntaryExits,
      List<Transfer> transfers) {
    return new BeaconBlockBody(
        randaoReveal,
        eth1Data,
        graffiti,
        ReadList.wrap(proposerSlashings, Integer::new),
        ReadList.wrap(attesterSlashings, Integer::new),
        ReadList.wrap(attestations, Integer::new),
        ReadList.wrap(deposits, Integer::new),
        ReadList.wrap(voluntaryExits, Integer::new),
        ReadList.wrap(transfers, Integer::new));
  }

  public BLSSignature getRandaoReveal() {
    return randaoReveal;
  }

  public Eth1Data getEth1Data() {
    return eth1Data;
  }

  public Bytes32 getGraffiti() {
    return graffiti;
  }

  public ReadList<Integer, ProposerSlashing> getProposerSlashings() {
    return proposerSlashings;
  }

  public ReadList<Integer, AttesterSlashing> getAttesterSlashings() {
    return attesterSlashings;
  }

  public ReadList<Integer, Attestation> getAttestations() {
    return attestations;
  }

  public ReadList<Integer, Deposit> getDeposits() {
    return deposits;
  }

  public ReadList<Integer, VoluntaryExit> getVoluntaryExits() {
    return voluntaryExits;
  }

  public ReadList<Integer, Transfer> getTransfers() {
    return transfers;
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
        && Objects.equal(graffiti, blockBody.graffiti)
        && Objects.equal(proposerSlashings, blockBody.proposerSlashings)
        && Objects.equal(attesterSlashings, blockBody.attesterSlashings)
        && Objects.equal(attestations, blockBody.attestations)
        && Objects.equal(deposits, blockBody.deposits)
        && Objects.equal(voluntaryExits, blockBody.voluntaryExits)
        && Objects.equal(transfers, blockBody.transfers);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        randaoReveal,
        eth1Data,
        graffiti,
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        transfers);
  }
}
