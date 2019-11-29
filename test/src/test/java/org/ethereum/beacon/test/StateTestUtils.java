package org.ethereum.beacon.test;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.model.BeaconStateData;
import org.ethereum.beacon.test.type.model.BlockData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.collections.Bitvector;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Various utility methods aiding state tests development. */
public abstract class StateTestUtils {
  private StateTestUtils() {}

  public static BeaconBlock parseBlockData(BlockData blockData, SpecConstants constants) {
    Eth1Data eth1Data1 = parseEth1Data(blockData.getBody().getEth1Data());

    // Attestations
    List<Attestation> attestations = new ArrayList<>();
    for (BeaconStateData.AttestationData attestationData :
        blockData.getBody().getAttestations()) {
      AttestationData attestationData1 = parseAttestationData(attestationData.getData());
      BytesValue aggValue = BytesValue.fromHexString(attestationData.getAggregationBits());
      Attestation attestation =
          new Attestation(
              Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().longValue()),
              attestationData1,
              BLSSignature.wrap(Bytes96.fromHexString(attestationData.getSignature())),
              constants);
      attestations.add(attestation);
    }

    // Attestation slashings
    List<AttesterSlashing> attesterSlashings =
        blockData.getBody().getAttesterSlashings().stream()
            .map(
                s ->
                    new AttesterSlashing(
                        parseSlashableAttestation(s.getSlashableAttestation1(), constants),
                        parseSlashableAttestation(s.getSlashableAttestation2(), constants)))
            .collect(Collectors.toList());

    // Deposits
    List<Deposit> deposits = new ArrayList<>();
    for (BlockData.BlockBodyData.DepositData depositData :
        blockData.getBody().getDeposits()) {
      Deposit deposit = parseDeposit(depositData);
      deposits.add(deposit);
    }

    // Proposer slashings
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    for (BlockData.BlockBodyData.ProposerSlashingData proposerSlashingData : blockData.getBody().getProposerSlashings()) {
      BeaconBlockHeader header1 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader1().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getBodyRoot()),
              BLSSignature.wrap(
                  Bytes96.fromHexString(proposerSlashingData.getHeader1().getSignature())));
      BeaconBlockHeader header2 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader2().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getBodyRoot()),
              BLSSignature.wrap(
                  Bytes96.fromHexString(proposerSlashingData.getHeader2().getSignature())));
      ProposerSlashing proposerSlashing =
          new ProposerSlashing(
              ValidatorIndex.of(proposerSlashingData.getProposerIndex()), header1, header2);
      proposerSlashings.add(proposerSlashing);
    }

    // Voluntary exits
    List<VoluntaryExit> voluntaryExits =
        blockData.getBody().getVoluntaryExits().stream()
            .map(StateTestUtils::parseVoluntaryExit)
            .collect(Collectors.toList());

    // Finally, creating a block
    BeaconBlockBody blockBody =
        new BeaconBlockBody(
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getBody().getRandaoReveal())),
            eth1Data1,
            Bytes32.fromHexString(blockData.getBody().getGraffiti()),
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            voluntaryExits,
            constants);
    BeaconBlock block =
        new BeaconBlock(
            SlotNumber.castFrom(UInt64.valueOf(blockData.getSlot())),
            Hash32.fromHexString(blockData.getParentRoot()),
            Hash32.fromHexString(blockData.getStateRoot()),
            blockBody,
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));

    return block;
  }

  public static IndexedAttestation parseSlashableAttestation(
      BlockData.BlockBodyData.IndexedAttestationData data, SpecConstants specConstants) {
    return new IndexedAttestation(
        data.getAttestingIndices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        parseAttestationData(data.getData()),
        data.getAggregateSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getAggregateSignature()))
            : BLSSignature.ZERO,
        specConstants);
  }

  public static MutableBeaconState parseBeaconState(
      SpecConstants specConstants, BeaconStateData data) {
    MutableBeaconState state = BeaconState.getEmpty(specConstants).createMutableCopy();

    state.setSlot(SlotNumber.castFrom(UInt64.valueOf(data.getSlot())));
    state.setGenesisTime(Time.of(data.getGenesisTime()));
    state.setFork(parseFork(data.getFork()));
    state.setPreviousJustifiedCheckpoint(parseCheckpoint(data.getPreviousJustifiedCheckpoint()));
    state.setCurrentJustifiedCheckpoint(parseCheckpoint(data.getCurrentJustifiedCheckpoint()));
    state.setJustificationBits(
        Bitvector.of(
            specConstants.getJustificationBitsLength(),
            BytesValue.fromHexString(data.getJustificationBits())));
    state.setFinalizedCheckpoint(parseCheckpoint(data.getFinalizedCheckpoint()));
    state.setLatestBlockHeader(parseBeaconBlockHeader(data.getLatestBlockHeader()));
    state.setEth1Data(parseEth1Data(data.getEth1Data()));
    state.setEth1DataVotes(
        WriteList.wrap(
            data.getEth1DataVotes().stream()
                .map(StateTestUtils::parseEth1Data)
                .collect(Collectors.toList()),
            Integer::new));
    state.setEth1DepositIndex(UInt64.valueOf(data.getEth1DepositIndex()));

    state.getValidators().replaceAll(parseValidatorRegistry(data.getValidators()));
    state.getBalances().replaceAll(parseBalances(data.getBalances()));
    state.getRandaoMixes().setAll(parseHashes(data.getRandaoMixes()));
    state
        .getPreviousEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getPreviousEpochAttestations(), specConstants));
    state
        .getCurrentEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getCurrentEpochAttestations(), specConstants));
    state.getSlashings().setAll(parseBalances(data.getSlashings()));

    return state;
  }

  public static List<PendingAttestation> parsePendingAttestations(
      List<BeaconStateData.AttestationData> data, SpecConstants constants) {
    return data.stream().map((BeaconStateData.AttestationData attestationData) -> parsePendingAttestation(attestationData, constants)).collect(Collectors.toList());
  }

  public static List<Hash32> parseHashes(List<String> data) {
    return data.stream().map(Hash32::fromHexString).collect(Collectors.toList());
  }

  public static List<Gwei> parseBalances(List<String> data) {
    return data.stream().map(b -> Gwei.castFrom(UInt64.valueOf(b))).collect(Collectors.toList());
  }

  public static List<ValidatorRecord> parseValidatorRegistry(List<BeaconStateData.ValidatorData> data) {
    return data.stream().map(StateTestUtils::parseValidatorRecord).collect(Collectors.toList());
  }

  public static ValidatorRecord parseValidatorRecord(BeaconStateData.ValidatorData data) {
    return new ValidatorRecord(
        BLSPubkey.fromHexString(data.getPubkey()),
        Hash32.fromHexString(data.getWithdrawalCredentials()),
        Gwei.castFrom(UInt64.valueOf(data.getEffectiveBalance())),
        data.getSlashed(),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEligibilityEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getExitEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getWithdrawableEpoch())));
  }

  public static Checkpoint parseCheckpoint(BeaconStateData.CheckpointData data) {
    return new Checkpoint(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        Hash32.fromHexString(data.getRoot()));
  }

  public static Eth1Data parseEth1Data(BlockData.BlockBodyData.Eth1 data) {
    return new Eth1Data(
        Hash32.fromHexString(data.getDepositRoot()),
        UInt64.valueOf(data.getDepositCount()),
        Hash32.fromHexString(data.getBlockHash()));
  }

  public static BeaconBlockHeader parseBeaconBlockHeader(BeaconStateData.BlockHeaderData data) {
    return new BeaconBlockHeader(
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        Hash32.fromHexString(data.getParentRoot()),
        Hash32.fromHexString(data.getStateRoot()),
        Hash32.fromHexString(data.getBodyRoot()),
        data.getSignature() == null
            ? BLSSignature.ZERO
            : BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static Deposit parseDeposit(BlockData.BlockBodyData.DepositData data) {
    return Deposit.create(
        data.getProof().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList()),
        new DepositData(
            BLSPubkey.fromHexString(data.getData().getPubkey()),
            Hash32.fromHexString(data.getData().getWithdrawalCredentials()),
            Gwei.castFrom(UInt64.valueOf(data.getData().getAmount())),
            BLSSignature.wrap(Bytes96.fromHexString(data.getData().getSignature()))));
  }

  public static Fork parseFork(BeaconStateData.Fork data) {
    return new Fork(
        Bytes4.fromHexString(data.getPreviousVersion()),
        Bytes4.fromHexString(data.getCurrentVersion()),
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())));
  }

  public static PendingAttestation parsePendingAttestation(
      BeaconStateData.AttestationData attestationData, SpecConstants constants) {
    BytesValue aggValue = BytesValue.fromHexString(attestationData.getAggregationBits());
    return new PendingAttestation(
        Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().getValue()),
        parseAttestationData(attestationData.getData()),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionDelay())),
        ValidatorIndex.of(attestationData.getProposerIndex()),
        constants);
  }

  public static AttestationData parseAttestationData(BeaconStateData.AttestationData.AttestationDataContainer data) {
    return new AttestationData(
        SlotNumber.of(data.getSlot()),
        CommitteeIndex.of(data.getIndex()),
        Hash32.fromHexString(data.getBeaconBlockRoot()),
        parseCheckpoint(data.getSource()),
        parseCheckpoint(data.getTarget()));
  }

  public static VoluntaryExit parseVoluntaryExit(
      BlockData.BlockBodyData.VoluntaryExitData data) {
    return new VoluntaryExit(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        ValidatorIndex.of(data.getValidatorIndex()),
        data.getSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getSignature()))
            : BLSSignature.ZERO);
  }
}
